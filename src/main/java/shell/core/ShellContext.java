package shell.core;

import lombok.Getter;
import lombok.Setter;
import shell.commands.Reflection;
import shell.commands.ShellCommand;
import shell.config.Config;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ShellContext {
    // Can be changed from different threads.
    private volatile boolean isRunning = true;
    private int shellExitCode = 0;

    private Path wd = Config.homeDirectory;

    private final Map<String, ShellCommand> shellCommands = Reflection.getShellCmds();

    private final Map<String, String> exportedEnvVar = new HashMap<>(System.getenv());
    private final Map<String, String> localEnvVars = new HashMap<>();
}
