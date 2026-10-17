package shell.parser.ast;

import lombok.extern.slf4j.Slf4j;
import shell.commands.ShellCommand;
import shell.core.ShellContext;
import shell.util.Util;

import java.io.*;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;

@Slf4j
public record CmdAstNode(List<String> args, List<RedirectOp> redirectors) implements AstNode {
    @Override
    public int execute(InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        log.debug("Executing: {}", args);

        // If it's a built-in command -> just execute it from the built-ins map and return.
        //todo CRITICAL!! I should pass it only after redirecting the streams.
        // And the ShellCommand.execute() method should be much dumber.
        ShellCommand builtIn = context.getShellCommands().get(args.getFirst());
        if (builtIn != null) {
            return builtIn.execute(
                    args.subList(1, args.size()),
                    in, out, err,
                    context
            );
        }

        ProcessBuilder pb = new ProcessBuilder()
                .command(args)
                .directory(new File(context.getWd().toString()));

        pb.environment().putAll(context.getExportedEnvVar());

        // If the provided streams are the default terminal streams,
        // instruct the OS kernel to wire the child process directly to the terminal.
        boolean inheritIn = (in == System.in);
        boolean inheritOut = (out == System.out);
        boolean inheritErr = (err == System.err);

        if (inheritIn) pb.redirectInput(ProcessBuilder.Redirect.INHERIT);
        if (inheritOut) pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        if (inheritErr) pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        /*
        File Redirection Overwrites.
        This loop will overwrite the inherit flags if the user
        explicitly redirected streams to files.

        When redirecting, there is no need to manually pump a file,
        ProcessBuilder will handle this natively.
         */
        for (RedirectOp op : redirectors) {
            Path targetPath = context.getWd().resolve(op.target()).normalize();
            File targetFile = targetPath.toFile();
            String targetName = op.target();

            //todo Move this logic to a new methods to make the code more readable.
            switch (op.opType()) {
                case REDIRECT_IN -> {
                    if (!targetFile.exists()) {
                        Util.println(err, "shell: " + targetName + ": No such file or directory");
                        return 1;
                    }
                    if (!targetFile.canRead()) {
                        Util.println(err, "shell: " + targetName + ": Permission denied");
                        return 1;
                    }
                    pb.redirectInput(targetFile);
                    inheritIn = true;
                }
                case REDIRECT_OUT, APPEND_REDIRECT_OUT, REDIRECT_ERR, APPEND_REDIRECT_ERR -> {
                    if (targetFile.isDirectory()) {
                        Util.println(err, "shell: " + targetName + ": Is a directory");
                        return 1;
                    }
                    if (targetFile.exists() && !targetFile.canWrite()) {
                        Util.println(err, "shell: " + targetName + ": Permission denied");
                        return 1;
                    }
                    if (!targetFile.exists()) {
                        File parent = targetFile.getParentFile();
                        if (parent != null && (!parent.exists() || !parent.canWrite())) {
                            Util.println(err, "shell: " + targetName + ": No such file or directory");
                            return 1;
                        }
                    }

                    switch (op.opType()) {
                        case REDIRECT_OUT -> {
                            pb.redirectOutput(targetFile);
                            inheritOut = true;
                        }
                        case APPEND_REDIRECT_OUT -> {
                            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(targetFile));
                            inheritOut = true;
                        }
                        case REDIRECT_ERR -> {
                            pb.redirectError(targetFile);
                            inheritErr = true;
                        }
                        case APPEND_REDIRECT_ERR -> {
                            pb.redirectError(ProcessBuilder.Redirect.appendTo(targetFile));
                            inheritErr = true;
                        }
                    }
                }
            }
        }

        try {
            Process process = pb.start();

            // I'll only spawn virtual threads for streams that are custom Java pipes.
            // If all were inherited, no threads are spawned.
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                if (!inheritIn) executor.submit(connectPipe(
                        in,
                        process.getOutputStream(),
                        true)
                );
                if (!inheritOut) executor.submit(connectPipe(
                        process.getInputStream(),
                        out,
                        false)
                );
                if (!inheritErr) executor.submit(connectPipe(
                        process.getErrorStream(),
                        err,
                        false)
                );
            }

            return process.waitFor();

        } catch (IOException e) {
            log.debug("Native execution failed for command: {}", args, e);

            int errno = Util.extractErrno(e.getMessage());
            String cmd = args.getFirst();

            switch (errno) {
                case 2 -> { // ENOENT
                    Util.println(err, "shell: command not found: " + cmd);
                    return 127;
                }
                case 13 -> { // EACCES
                    Util.println(err, "shell: " + cmd + ": Permission denied");
                    return 126;
                }
                case 1 -> { // EPERM
                    Util.println(err, "shell: Operation not permitted: " + cmd);
                    return 126;
                }
                case 21 -> { // EISDIR
                    Util.println(err, "shell: " + cmd + " Is a directory");
                    return 126;
                }
                default -> { // -1 or unmapped
                    String rawMsg = e.getMessage();
                    String cleanOsMsg = "execution failed";

                    if (rawMsg != null && rawMsg.contains(", ")) {
                        cleanOsMsg = rawMsg.substring(rawMsg.indexOf(", ") + 2);
                    }

                    Util.println(err, "shell: " + cmd + ": " + cleanOsMsg);
                    return 1;
                }
            }
        } catch (InterruptedException e) {
            log.error("Virtual thread pumping interrupted for command {}", args, e);
            Thread.currentThread().interrupt();
            return 130;
        } catch (Exception e) {
            log.error("Unexpected JVM error during execution of: {}", args, e);
            Util.println(err, "shell: execution failed");
            return 1;
        }
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
