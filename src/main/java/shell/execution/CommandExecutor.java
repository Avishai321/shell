package shell.execution;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import shell.commands.ShellCommand;
import shell.core.ShellContext;
import shell.util.Util;

import java.io.*;
import java.util.List;
import java.util.concurrent.Executors;

/*
 * The core Execution Engine
 *
 * This class isolates the complexity of OS-level and process management
 * away from the AST Parser.
 * It takes a parsed command and validated I/O state, and executes it.
 */
@Slf4j
@UtilityClass
public class CommandExecutor {
    public static int executeCommand(
            List<String> args,
            StreamState streamState,
            ShellContext context) {

        String command = args.getFirst();
        ShellCommand builtIn = context.getShellCommands().get(command);

        if (builtIn != null) {
            return executeBuiltIn(builtIn, args, streamState, context);
        }

        return executeNative(args, streamState, context);
    }

    private static int executeBuiltIn(
            ShellCommand builtIn,
            List<String> args,
            StreamState streamState,
            ShellContext context) {

        // Default to terminal streams.
        InputStream in = streamState.terminalIn;
        OutputStream out = streamState.terminalOut;
        OutputStream err = streamState.terminalErr;

        try {
            // Translate StreamState Files into Java IO Streams for Built-ins
            if (streamState.redirectIn != null) {
                in = new FileInputStream(streamState.redirectIn);
            }
            if (streamState.redirectOut != null) {
                out = new FileOutputStream(streamState.redirectOut, streamState.appendOut);
            }
            if (streamState.redirectErr != null) {
                err = new FileOutputStream(streamState.redirectErr, streamState.appendErr);
            }

            // I pass the locally modified 'in/out/err' streams,
            // so the built-in writes to the files.
            return builtIn.execute(
                    //todo IMPORTANT I'm going to change the builtIn.execute() method to accept the
                    // args as they are, so I won't have to subList the args.
                    args.subList(1, args.size()),
                    in, out, err,
                    context
            );

        } catch (IOException e) {
            log.error("Failed to open redirection streams for built-in command: {}", args, e);
            Util.println(streamState.terminalErr, "shell: " + e.getMessage());
            return 1;

        } finally {
            // CRITICAL: Prevent file locks. I only close streams if I created them for files.

            // CRITICAL: Prevent OS file lock leaks.
            // I must strictly close only the streams I manually created for files,
            // and never the System terminal stream.
            try {
                if (streamState.redirectIn != null && in != null) in.close();
                if (streamState.redirectOut != null && out != null) out.close();
                if (streamState.redirectErr != null && err != null) err.close();
            } catch (IOException ignore) {
            }
        }
    }

    private static int executeNative(
            List<String> args,
            StreamState state,
            ShellContext context) {

        ProcessBuilder pb = new ProcessBuilder()
                .command(args)
                .directory(new File(context.getWd().toString()));

        pb.environment().putAll(context.getExportedEnvVar());

        // Delegate the complex ProcessBuilder I/O mapping to keep this method clean.
        boolean[] inheritFlags = applyNativeRedirections(pb, state);
        boolean inheritIn = inheritFlags[0];
        boolean inheritOut = inheritFlags[1];
        boolean inheritErr = inheritFlags[2];

        try {
            Process process = pb.start();

            // I'll only spawn virtual threads for streams that are custom Java pipes.
            // If all were inherited, no threads are spawned.
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                if (!inheritIn) executor.submit(connectPipe(
                        state.terminalIn, process.getOutputStream(), true
                ));
                if (!inheritOut) executor.submit(connectPipe(
                        process.getInputStream(), state.terminalOut, false
                ));
                if (!inheritErr) executor.submit(connectPipe(
                        process.getErrorStream(), state.terminalErr, false
                ));
            }

            return process.waitFor();

        } catch (IOException e) {
            log.debug("Native execution failed for command: {}", args, e);

            int errno = Util.extractErrno(e.getMessage());
            String cmd = args.getFirst();

            switch (errno) {
                case 2 -> { // ENOENT
                    Util.println(state.terminalErr, "shell: command not found: " + cmd);
                    return 127;
                }
                case 13 -> { // EACCES
                    Util.println(state.terminalErr, "shell: " + cmd + ": Permission denied");
                    return 126;
                }
                case 1 -> { // EPERM
                    Util.println(state.terminalErr, "shell: Operation not permitted: " + cmd);
                    return 126;
                }
                case 21 -> { // EISDIR
                    Util.println(state.terminalErr, "shell: " + cmd + " Is a directory");
                    return 126;
                }
                default -> { // -1 or unmapped
                    String rawMsg = e.getMessage();
                    String cleanOsMsg = "execution failed";

                    if (rawMsg != null && rawMsg.contains(", ")) {
                        cleanOsMsg = rawMsg.substring(rawMsg.indexOf(", ") + 2);
                    }

                    Util.println(state.terminalErr, "shell: " + cmd + ": " + cleanOsMsg);
                    return 1;
                }
            }
        } catch (InterruptedException e) {
            log.error("Virtual thread pumping interrupted for command {}", args, e);
            Thread.currentThread().interrupt();
            return 130; // Standard POSIX code for SIGINT (128 + 2)
        } catch (Exception e) {
            log.error("Unexpected JVM error during execution of: {}", args, e);
            Util.println(state.terminalErr, "shell: execution failed");
            return 1;
        }
    }

    /*
     * Maps the StreamState into the ProcessBuilder.
     * Returns a boolean array: [inheritIn, inheritOut, inheritErr]
     * True means the OS kernel handles the I/O natively (no virtual threads needed).
     */
    private static boolean[] applyNativeRedirections(ProcessBuilder pb, StreamState state) {
        boolean inheritIn = false;
        boolean inheritOut = false;
        boolean inheritErr = false;

        // STDIN
        if (state.redirectIn != null) {
            pb.redirectInput(state.redirectIn);
            inheritIn = true;
        } else if (state.terminalIn == System.in) {
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
            inheritIn = true;
        }

        // STDOUT
        if (state.redirectOut != null) {
            pb.redirectOutput(
                    state.appendOut
                            ? ProcessBuilder.Redirect.appendTo(state.redirectOut)
                            : ProcessBuilder.Redirect.to(state.redirectOut)
            );
            inheritOut = true;
        } else if (state.terminalOut == System.out) {
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            inheritOut = true;
        }

        // STDERR
        if (state.redirectErr != null) {
            pb.redirectError(
                    state.appendErr
                            ? ProcessBuilder.Redirect.appendTo(state.redirectErr)
                            : ProcessBuilder.Redirect.to(state.redirectErr)
            );
            inheritErr = true;
        } else if (state.terminalErr == System.err) {
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            inheritErr = true;
        }

        return new boolean[]{inheritIn, inheritOut, inheritErr};
    }

    private Runnable connectPipe(InputStream source, OutputStream target, boolean closeTarget) {
        return () -> {
            try {
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = source.read(buffer)) != -1) {
                    target.write(buffer, 0, bytesRead);
                    target.flush();
                }
            } catch (IOException e) {
                log.debug("{} stream pumping  interrupted or closed", source);
            } finally {
                try {
                    source.close();
                } catch (IOException ignore) {
                }
                if (closeTarget) {
                    try {
                        target.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        };
    }
}
