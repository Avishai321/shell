package shell.commands;

import shell.core.ShellContext;
import shell.util.Util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Command(name = "exit")
public class ExitCommand implements ShellCommand {
    @Override
    public int execute(List<String> args, InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        /*
         * Accepts zero or one argument which sets the shell exit code.
         */

        // Defaults to the last executed command's status.
        int code = context.getShellExitCode();

        if (args.size() > 1) {
            try {
                code = Integer.parseInt(args.get(1));
                if (args.size() > 2) {
                    Util.println(err, "exit: too many arguments.");
                    return 1; // Fail but does not exit the shell
                }
            } catch (NumberFormatException e) {
                Util.println(err, "exit: " + args.get(1) + ": numeric argument required");
                context.setShellExitCode(255);

            }
        }

        context.setShellExitCode(code);
        context.setRunning(false);
        return code;
    }
}
