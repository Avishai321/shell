package shell.config;

import lombok.experimental.UtilityClass;
import shell.util.Util;

import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public class Config {
    public final Path homeDirectory = Paths.get(System.getProperty("user.home"));
    public final String homeDirectoryStr = homeDirectory.toString();

    public final String emoji = "\uD83D\uDC27";
    public final String userName = System.getProperty("user.name");
    public final String osName = Util.getPrettyOsName();
}
