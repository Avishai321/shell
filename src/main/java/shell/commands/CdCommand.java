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
        //todo Check if cd should accept flags
        // Right now it treats the first arg as the path and ignores the rest.

        // "cd" with no path -> goes to the home directory.
        if (args.isEmpty()) {
            context.setWd(Config.homeDirectory);
            return 0;
        }

        String path = args.getFirst();

        // Absolute vs Relative path.
        Path target = path.startsWith("/")
                ? Path.of(path)
                : context.getWd().resolve(path).normalize();

        if (!Files.exists(target)) {
            Util.println(err, "cd: no such file or directory: " + target);
            return 1;
        }

        if (!Files.isDirectory(target)) {
            Util.println(err, "cd: not a directory" + target);
            return 1;
        }

        //todo handle permission denied

        //todo Is it really safe to set it after the security checks above? or I need more checks?
        context.setWd(target);
        return 0;
    }
}
