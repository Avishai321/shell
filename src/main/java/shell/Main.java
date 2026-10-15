package shell;

import lombok.extern.slf4j.Slf4j;
import shell.core.Shell;

@Slf4j
public class Main {
    public static void main(String[] args) {
        //todo
        // * Add documentation and journey development and thoughts!!!!
        // * Use Util.writeToOutputStream (change the method name) instead of local writeErr methods.
        // * Update the lexer and parser with the $ char, and learn EXACTLY what it should do.
        // * Create doc/Abstract Syntax Tree (or doc/Parser) file, explaining about the parsing job.
        //      Do the same for the Lexer as for now these file has no proper documentation.
        log.debug("Starting application");
        new Shell().start();
    }
}
