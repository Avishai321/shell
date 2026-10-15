package shell.parser.ast;

import lombok.extern.slf4j.Slf4j;
import shell.core.ShellContext;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.util.concurrent.CompletableFuture;

@Slf4j
public record PipeAstNode(AstNode left, AstNode right) implements AstNode {
    @Override
    public int execute(InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        try {
            //todo add documentation here.
            Pipe osPipe = Pipe.open();

            OutputStream pipOut = Channels.newOutputStream(osPipe.sink());
            InputStream pipIn = Channels.newInputStream(osPipe.source());

            CompletableFuture<Integer> leftFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return left.execute(in, pipOut, err, context);
                } finally {
                    try {
                        pipOut.close();
                    } catch (IOException e) {
                        log.error("Failed to close pipeline write end", e);
                    }
                }
            });

            int rightExitCode = right.execute(pipIn, out, err, context);
            leftFuture.join();
            pipIn.close();
            return rightExitCode;

        } catch (Exception e) {
            log.error("Pipeline execution error.", e);
            try {
                err.write("shell: pipeline execution error\n".getBytes());
            } catch (IOException ignored) {
            }
            return 1;
        }
    }
}
