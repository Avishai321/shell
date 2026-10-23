package shell.commands;

import shell.core.ShellContext;
import shell.util.Util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Command(name = "echo")
public class EchoCommand implements ShellCommand {
    // I'm trying to save time by implementing simple commands myself
    // instead of spinning up a new process for such operations.
    // 'echo' is a real sh, and it means that it will work perfectly
    // even without my implementation.
    @Override
    public int execute(List<String> args, InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        //todo Check what flags echo accepts.

        // If there are no args -> print a new line.
        if (args.size() == 1) {
            Util.println(out, "");
        }

        String output = String.join(" ", args.subList(1, args.size()));
        return Util.println(out, output);
    }
}
