package shell.commands;

import shell.core.ShellContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/*
commands to implement:
* cd (change directory...)
* export (environment variables)
* alias & unalias //todo Create a new map for them in ShellContext
* pwd (print working directory)
* echo (print the arguments as they are)
* exit (exit...)

I'm trying to save time by implementing simple commands myself
    instead of spinning up a new process for such operations.
'echo' & 'pwd' are real executables, and it means that it will work perfectly
    even without my implementation.
 */

//TODO The args should get the command name itself in the first position.
// Make sure to update all the implementations and usages with that!.
public interface ShellCommand {
    int execute(List<String> args,
                InputStream in,
                OutputStream out,
                OutputStream err,
                ShellContext context
    );
}
