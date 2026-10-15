package shell.commands;

import shell.core.ShellContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Command(name = "unalias")
public class UnaliasCommand implements ShellCommand {
    @Override
    public int execute(List<String> args, InputStream in, OutputStream out, OutputStream err, ShellContext context) {

        return 0;
    }
}
