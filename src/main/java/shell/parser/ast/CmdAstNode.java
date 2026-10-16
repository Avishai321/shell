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
            switch (op.opType()) {
                case REDIRECT_IN -> {
                    pb.redirectInput(targetFile);
                    inheritIn = true;
                }
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
            //todo this exception is thrown in more than one case,
            // it's wrong to decide that it's command not found error without checking first.
            Util.write(err, "shell: command not found: " + args.getFirst(), true);
            return 127;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return 1;
        } catch (Exception e) {
            Util.write(err, "shell: execution failed", true);
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
