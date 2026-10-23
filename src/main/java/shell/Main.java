package shell;

import lombok.extern.slf4j.Slf4j;
import shell.core.Shell;

@Slf4j
public class Main {
    public static void main(String[] args) {
        //TODO: *CRITICAL*
        // *

        //TODO:
        // * Fix EchoCommand Argument Truncation:
        //      EchoCommand currently hardcodes args.get(1) instead of iterating through
        //      args from 1 to the end, joining them with spaces before printing.
        // * Add documentation and journey development and thoughts!!!!
        // * Update the lexer and parser with the $ char, and learn EXACTLY what it should do.
        // * In some places I'm catching more than one Exception throw in the same catch block,
        //      that means that I can print the same error for two different ones.
        // * Reduce the emojis usage so the shell will be rendered in more terminals.
        // * Commands in Windows are never found (Check it again after improving the exception handling).
        // * Implement the Alias/Unalias Command's execute() method.

        log.debug("Starting application");
        new Shell().start();
    }
}
