package shell.commands;

import shell.core.ShellContext;
import shell.util.Util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Command(name = "unalias")
public class UnaliasCommand implements ShellCommand {
    @Override
    public int execute(List<String> args, InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        if (args.size() == 1) {
            Util.println(err, "unalias: usage: unalias [-a] name [name ...]");
            return 1;
        }

        Map<String, String> aliases = context.getAliases();

        // Clear all aliases.
        if (args.get(1).equals("-a")) {
            aliases.clear();
            return 0;
        }

        int exitCode = 0;

        for (int i = 1; i < args.size(); i++) {
            String arg = args.get(i);
            if (aliases.remove(arg) == null) {
                Util.println(err, "shell: unalias: " + arg + ": not found");
                exitCode = 1;
            }
        }

        return exitCode;
    }
}
