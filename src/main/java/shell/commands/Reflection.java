package shell.commands;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class Reflection {
    /*
    Reflect through all .class files
    Find those in the commands package
    which annotated with @Command AND capable of implementing the ShellCommand interface.
    Then it maps every shell-command to the map:
        Key: Command.name()
        Value: ShellCommand instance
     */
    public static Map<String, ShellCommand> getShellCmds() {
        Map<String, ShellCommand> map = new HashMap<>();

        String packageName = "shell.commands";
        String path = packageName.replace(".", "/");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        try {
            Enumeration<URL> resources = classLoader.getResources(path);
            List<File> directories = new ArrayList<>();

            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                directories.add(new File(resource.getFile()));
            }

            directories.stream()
                    .filter(File::exists)
                    .flatMap(Reflection::streamFilesInDir)
                    .filter(file -> file.endsWith(".class"))
                    .map(file -> getClassByName(packageName, file))
                    .filter(clazz -> clazz.isAnnotationPresent(Command.class))
                    .filter(ShellCommand.class::isAssignableFrom)
                    .forEach(clazz -> Reflection.putCommand(map, clazz));
        } catch (Exception e) {
            log.error("Failed to scan for shell commands", e);
        }

        return map;
    }

    private static Stream<String> streamFilesInDir(File directory) {
        String[] files = directory.list();
        return files == null
                ? Stream.empty()
                : Arrays.stream(files);
    }

    private static Class<?> getClassByName(String packageName, String fileName) {
        String qualifiedClassName = packageName + "."
                + fileName.substring(0, fileName.length() - 6);

        try {
            return Class.forName(qualifiedClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load class: " + fileName, e);
        }
    }

    private static void putCommand(Map<String, ShellCommand> map, Class<?> clazz) {
        try {
            String cmdName = clazz.getAnnotation(Command.class).name();
            Object instance = clazz.getDeclaredConstructor().newInstance();

            map.put(cmdName, (ShellCommand) instance);
            log.debug("Registered built-in command: {}", cmdName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate command class: " + clazz.getName(), e);
        }
    }
}
