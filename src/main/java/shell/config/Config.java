package shell.config;

import lombok.experimental.UtilityClass;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;

@UtilityClass
public class Config {
    public final Path homeDirectory = Paths.get(System.getProperty("user.home"));
    public final String homeDirectoryStr = homeDirectory.toString();

    public final String userName = System.getProperty("user.name");
    public final String osName = getPrettyOsName();

    private String getPrettyOsName() {
        String osName = System.getProperty("os.name");

        // If it's a Linux OS -> find the distro name from /etc/os-release.
        if (osName.equalsIgnoreCase("linux")) {
            try (BufferedReader reader = new BufferedReader(new FileReader("/etc/os-release"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("PRETTY_NAME=")) {
                        String prettyName = line.replace("PRETTY_NAME=", "")
                                .replace("\"", "")
                                .toLowerCase();

                        if (!prettyName.isBlank()) {
                            osName = prettyName;
                            break;
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }

        return osName.trim().replace(" ", "-");
    }
}
