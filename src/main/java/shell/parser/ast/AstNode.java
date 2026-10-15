package shell.parser.ast;

import shell.core.ShellContext;

import java.io.InputStream;
import java.io.OutputStream;

public interface AstNode {
    /**
     * @param in      StdIn (System.in by default)
     * @param out     StdOut (System.out by default)
     * @param err     StdErr (System.err by default)
     * @param context encapsulates the state of the shell (env vars, wd, ...)
     * @return exit code (0 for success, 1-255 for various errors)
     */
    int execute(InputStream in, OutputStream out, OutputStream err, ShellContext context);
}
