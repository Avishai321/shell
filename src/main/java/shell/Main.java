package shell;

import lombok.extern.slf4j.Slf4j;
import shell.core.Shell;

@Slf4j
public class Main {
    public static void main(String[] args) {
        //TODO: *CRITICAL*
        // *

        //TODO:
        // * Separate the Util class and make it smaller, maybe add a ui or visual package
        //      to handle formatting and ui shit.
        // * Add documentation and journey development and thoughts!!!!
        // * Add doc for Redirections.txt explaining about the redirectors, because it becomes
        //      complex and an important part of the application.
        // * Update the lexer and parser with the $ char, and learn EXACTLY what it should do.
        // * In some places I'm catching more than one Exception throw in the same catch block,
        //      that means that I can print the same error for two different ones.
        // * Add log.error() in every catch block.
        // * Reduce the emojis usage so the shell will be rendered in more terminals.
        // * Commands in Windows are never found (Check it again after improving the exception handling).
        // * Implement the Alias/Unalias Command's execute() method.

        log.debug("Starting application");
        new Shell().start();
    }
}
