package shell;

import lombok.extern.slf4j.Slf4j;
import shell.core.Shell;

@Slf4j
public class Main {
    public static void main(String[] args) {
        //TODO: *CRITICAL*
        // * Append redirect output is not working.
        // * Redirection is not working at all.
        //      Probably the new method in the execution package
        // * While fixing all the redirection problems, it is important to also add support
        //      For all different redirect methods (e.g., '>', '1>', '2>', ...).

        //TODO:
        // * Separate the Util class and make it smaller, maybe add a ui or visual package
        //      to handle formatting and ui shit.
        // * Add documentation and journey development and thoughts!!!!
        // * Add doc for Redirections.txt explaining about the redirectors, because it becomes
        //      complex and an important part of the application.
        // * Update the lexer and parser with the $ char, and learn EXACTLY what it should do.
        // * Create doc/Abstract Syntax Tree (or doc/Parser) file, explaining about the parsing job.
        //      Do the same for the Lexer as for now these file has no proper documentation.
        // * In some places I'm catching more than one Exception throw in the same catch block,
        //      that means that I can print the same error for two different ones.
        // * Add log.error() in every catch block.
        // * Reduce the emojis usage so the shell will be rendered in more terminals.
        // * Commands in Windows are never found (Check it again after improving the exception handling).
        // * Make the ShellCommand accept the command itself in the args to simplify execution.
        //      Right now, if I have a command with it's args and I want to execute a built-in -
        //      then I'll have to pass the sublist, it's confusing and feels wrong.
        // * Implement the Alias/Unalias Command's execute() method.

        log.debug("Starting application");
        new Shell().start();
    }
}
