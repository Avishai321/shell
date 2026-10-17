package shell.execution;

import lombok.experimental.UtilityClass;
import shell.core.ShellContext;
import shell.lexer.Lexer;
import shell.parser.ast.RedirectOp;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/*
* The "Pre-Flight" Redirection Engine.
*
* Native processes and Java Built-ins handle I/O completely differently.
* I cannot blindly pass redirect operators to them.
* This class intercepts all operators, validates the filesystem permissions in advance,
* and populates a unified StreamState so the Executor can route the I/O correctly.
 */
@UtilityClass
public class RedirectionResolver {
    public static void resolve(
            List<RedirectOp> redirectors,
            StreamState state,
            ShellContext context) throws RedirectionException {

        // Iterate through all redirections.
        // Later operators will overwrite earlier ones.
        for (RedirectOp op : redirectors) {
            Path targetPath = context.getWd().resolve(op.target()).normalize();
            File targetFile = targetPath.toFile();
            String targetName = op.target();

            switch (op.opType()) {
                case REDIRECT_IN -> {
                    if (!targetFile.exists()) throw new RedirectionException(
                            "shell: " + targetName + ": No such file or directory"
                    );
                    if (!targetFile.canRead()) throw new RedirectionException(
                            "shell: " + targetName + ": Permission denied"
                    );

                    state.redirectIn = targetFile;
                }

                case REDIRECT_OUT, APPEND_REDIRECT_OUT, REDIRECT_ERR, APPEND_REDIRECT_ERR -> {
                    if (targetFile.isDirectory()) throw new RedirectionException(
                            "shell: " + targetName + ": Is a directory"
                    );
                    if (targetFile.exists() && !targetFile.canWrite()) throw new RedirectionException(
                            "shell: " + targetName + ": Permission denied"
                    );

                    // If the file doesn't exist, ensure the parent directory allows to create it.
                    if (!targetFile.exists()) {
                        File parent = targetFile.getParentFile();
                        if (parent != null && (!parent.exists() || !parent.canWrite())) {
                            throw new RedirectionException(
                                    "shell: " + targetName + ": No such file or directory");
                        }
                    }

                    switch (op.opType()) {
                        case REDIRECT_OUT, APPEND_REDIRECT_OUT -> {
                            state.redirectOut = targetFile;
                            state.appendOut = (op.opType() == Lexer.TokenType.APPEND_REDIRECT_OUT);
                        }
                        case REDIRECT_ERR, APPEND_REDIRECT_ERR -> {
                            state.redirectErr = targetFile;
                            state.appendErr = (op.opType() == Lexer.TokenType.APPEND_REDIRECT_ERR);
                        }
                    }
                }
            }
        }
    }
}
