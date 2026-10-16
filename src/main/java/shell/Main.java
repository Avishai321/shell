package shell;

import lombok.extern.slf4j.Slf4j;
import shell.core.Shell;

@Slf4j
public class Main {
    public static void main(String[] args) {
        //todo CRITICAL
        // * redirectors are not working as expected.
        //      Check for different redirect methods, I saw 1> for stdout redirection, just like '>'.

        //todo
        // * Add documentation and journey development and thoughts!!!!
        // * Update Util.write to Util.println & Util.print.
        // * Use Util.writeToOutputStream (change the method name) instead of local writeErr methods.
        // * Update the lexer and parser with the $ char, and learn EXACTLY what it should do.
        // * Create doc/Abstract Syntax Tree (or doc/Parser) file, explaining about the parsing job.
        //      Do the same for the Lexer as for now these file has no proper documentation.
        // * In some places I'm catching more than one Exception throw in the same catch block,
        //      that means that I can print the same error for two different ones.
        // * Add log.error() in every catch block.
        // * Reduce the emojis usage so the shell will be rendered in more terminals.
        // * Commands in Windows are never found (Check it again after improving the exception handling).

        log.debug("Starting application");
        new Shell().start();
    }
}
