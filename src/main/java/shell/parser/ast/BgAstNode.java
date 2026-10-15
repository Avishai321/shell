package shell.parser.ast;

import shell.core.ShellContext;

import java.io.InputStream;
import java.io.OutputStream;

public record BgAstNode(AstNode next) implements AstNode {
    @Override
    public int execute(InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        //todo I've seen in other shells - output from a background job INSIDE the terminal.
        // So I might shouldn't mess with the err or out streams, check about this.
        // I'm guessing it's a bug in my shell (zsh) but it worth checking it.
        new Thread(() -> next.execute(
                InputStream.nullInputStream(),
                OutputStream.nullOutputStream(),
                OutputStream.nullOutputStream(),
                context
        )).start();
        return 0;
    }
}
