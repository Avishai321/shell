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
        if (args.size() > 1) {
            try {
                int code = Integer.parseInt(args.get(1));
                if (args.size() > 2) {
                    Util.println(err, "exit: too many arguments.");
                    return 1;
                }
                context.setShellExitCode(code);
            } catch (NumberFormatException e) {
                Util.println(err, "exit: " + args.get(1) + ": numeric argument required");
                context.setShellExitCode(255);

            }
        }
        context.setRunning(false);
        return 0;
    }
}
