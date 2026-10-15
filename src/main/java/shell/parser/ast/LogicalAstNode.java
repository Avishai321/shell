package shell.parser.ast;

import shell.core.ShellContext;
import shell.lexer.Lexer;

import java.io.InputStream;
import java.io.OutputStream;

import static shell.lexer.Lexer.TokenType.*;

public record LogicalAstNode(Lexer.TokenType type, AstNode left, AstNode right) implements AstNode {
    @Override
    public int execute(InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        int leftExitCode = left.execute(in, out, err, context);

        // Check if the left command terminated the shell.
        // Example: exit && echo "I'm still alive" -> the echo shouldn't execute.
        if (!context.isRunning()) {
            return leftExitCode;
        }

        boolean executeRight = (this.type == AND && leftExitCode == 0)
                || (this.type == OR && leftExitCode != 0);

        return executeRight
                ? right.execute(in, out, err, context)
                : leftExitCode;
    }
}
