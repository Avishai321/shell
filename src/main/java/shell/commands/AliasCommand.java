package shell.commands;

import shell.core.ShellContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Command(name = "alias")
public class AliasCommand implements ShellCommand {
    @Override
    public int execute(List<String> args, InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        //todo complete implementation.
        return 0;
    }
}
