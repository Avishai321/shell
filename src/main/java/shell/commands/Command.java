package shell.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
Every class annotated with @Command, and implements the ShellCommand interface
will be added to ShellContext.shellCommands via reflection.

The name variable will become the key to the ShellCommand in the ShellContext.shellCommands map
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Command {
    String name();
}
