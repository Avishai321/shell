package shell.commands;

import shell.config.Config;
import shell.core.ShellContext;
import shell.util.Util;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Command(name = "cd")
public class CdCommand implements ShellCommand {
    @Override
    public int execute(List<String> args, InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        // "cd" with no path -> goes to the home directory.
        if (args.size() == 1) {
            context.setWd(Config.homeDirectory);
            return 0;
        }

        String path = args.get(1);

        // Absolute vs Relative path.
        Path target = path.startsWith("/")
                ? Path.of(path)
                : context.getWd().resolve(path).normalize();

        if (!Files.exists(target)) {
            Util.println(err, "cd: no such file or directory: " + target);
            return 1;
        }

        if (!Files.isDirectory(target)) {
            Util.println(err, "cd: not a directory: " + target);
            return 1;
        }

        if (!Files.isExecutable(target)) {
            Util.println(err, "cd: permission denied: " + target);
            return 1;
        }

        context.setWd(target);
        return 0;
    }
}
