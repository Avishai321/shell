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
    private volatile boolean isRunning;
    private volatile int shellExitCode;

    private Path wd;

    private final Map<String, ShellCommand> shellCommands;

    private final Map<String, String> exportedEnvVar;
    private final Map<String, String> localEnvVars;

    public ShellContext() {
        this.isRunning = true;
        this.shellExitCode = 0;
        this.wd = Config.homeDirectory;
        this.shellCommands = Reflection.getShellCmds();
        this.exportedEnvVar = new HashMap<>(System.getenv());
        this.localEnvVars = new HashMap<>();

    }

    // Copy constructor for subshells (background jobs and pipelines)
    public ShellContext(ShellContext other) {
        this.isRunning = other.isRunning;
        this.shellExitCode = other.shellExitCode;
        this.wd = other.wd;
        this.shellCommands = other.shellCommands; // Share immutable command map
        this.exportedEnvVar = new HashMap<>(other.exportedEnvVar); // Isolate map
        this.localEnvVars = new HashMap<>(other.localEnvVars);     // Isolate map
    }
}
