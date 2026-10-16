package shell.commands;

import shell.core.ShellContext;
import shell.util.Util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Command(name = "pwd")
public class PwdCommand implements ShellCommand {
    @Override
    public int execute(List<String> args, InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        //todo Implement pwd flags
        return Util.println(out, context.getWd().toString());
    }
}
