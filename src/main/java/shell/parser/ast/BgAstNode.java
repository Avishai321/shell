package shell.parser.ast;

import shell.core.ShellContext;

import java.io.InputStream;
import java.io.OutputStream;

public record BgAstNode(AstNode next) implements AstNode {
    @Override
    public int execute(InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        /*
         * POSIX standard dictates that background jobs inherit STDOUT and STDERR
         * from the parent unless explicitly redirected.
         * But they must be disconnected from STDIN so they do not steal keyboard input.
         */
        new Thread(() -> next.execute(
                InputStream.nullInputStream(),
                out, err,
                context
        )).start();
        return 0;
    }
}
