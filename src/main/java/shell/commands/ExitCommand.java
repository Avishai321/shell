package shell.commands;

import shell.core.ShellContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Command(name = "exit")
public class ExitCommand implements ShellCommand {
    @Override
    public int execute(List<String> args, InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        //todo Check exactly what exit should do and if it should accept flags.
        context.setRunning(false);
        return 0;
    }
}
