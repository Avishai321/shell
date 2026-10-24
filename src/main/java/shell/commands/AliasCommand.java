package shell.commands;

import shell.core.ShellContext;
import shell.util.Util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Command(name = "alias")
public class AliasCommand implements ShellCommand {
    @Override
    public int execute(List<String> args, InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        Map<String, String> aliases = context.getAliases();

        // Print all aliases.
        if (args.size() == 1) {
            // A TreeMap auto sorts the keys.
            Map<String, String> sortedAliases = new TreeMap<>(aliases);
            for (Map.Entry<String, String> entry : sortedAliases.entrySet()) {
                Util.println(out, formatAliasOutput(entry.getKey(), entry.getValue()));
            }
            return 0;
        }

        int exitCode = 0;

        // Iterate through all arguments to support chaining (e.e., alias a=b c=d).
        for (int i = 1; i < args.size(); i++) {
            String arg = args.get(i);
            int eqIndex = arg.indexOf('=');

            if (eqIndex == -1) {
                // Query an existing alias.
                String val = aliases.get(arg);
                if (val == null) {
                    Util.println(err, "alias: " + arg + " not found");
                    exitCode = 1;
                } else {
                    Util.println(out, formatAliasOutput(arg, val));
                }
            } else if (eqIndex == 0) {
                // Catch bad assignments like "alias =" or "alias =val"
                Util.println(err, "alias: bad assignment");
                exitCode = 1;
            } else {
                // Assign a new alias
                String name = arg.substring(0, eqIndex);
                String value = arg.substring(eqIndex + 1);
                aliases.put(name, value);
            }
        }

        return exitCode;
    }

    //Result example:
    // something=someValue
    // test='some other value'
    private String formatAliasOutput(String name, String value) {
        // Inverse Safelist: If the string contains anything other than
        // these specific safe characters, it requires quoting.
        boolean needsQuotes = value.isEmpty() || !value.matches("^[a-zA-Z0-9_.,/+-]+$");

        if (!needsQuotes) {
            return name + "=" + value;
        }

        // Serialization: I must escape existing single quotes
        // to prevent them from terminating our outer wrapper.
        // A single quote in POSIX serialization is written as: '\''
        String escapedValue = value.replace("'", "'\\''");
        return name + "='" + escapedValue + "'";
    }
}
