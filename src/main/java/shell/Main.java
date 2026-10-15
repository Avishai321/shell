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

        //todo
        // found a bug:
        // ┌──(avishai🐧tuxedo os)-[/]                       5ms  
        // └─$ grep -r "StringToFind" /var > matches.txt
        // Tokens list: [Token[type=STRING, val=grep], Token[type=STRING, val=-r], Token[type=STRING, val=StringToFind], Token[type=STRING, val=/var], Token[type=REDIRECT_OUT, val=>], Token[type=STRING, val=matches.txt]]
        // Abstract Syntax Tree:
        // AST ROOT
        // └── COMMAND: [grep, -r, StringToFind, /var] | REDIRECTS: [RedirectOp[opType=REDIRECT_OUT, target=matches.txt]]
        // Executing: [grep, -r, StringToFind, /var]
        // shell: command not found: grep
        // ┌──(avishai🐧tuxedo os)-[/]                                                                                                        0ms  
        // └─$ grep -r "StringToFind" /var
        // Tokens list: [Token[type=STRING, val=grep], Token[type=STRING, val=-r], Token[type=STRING, val=StringToFind], Token[type=STRING, val=/var]]
        // Abstract Syntax Tree:
        // AST ROOT
        // └── COMMAND: [grep, -r, StringToFind, /var]
        // Executing: [grep, -r, StringToFind, /var]
        // grep: /var/log/boot.log.2: Permission denied
        // grep: /var/log/private: Permission denied
        // ...

        //todo
        // another bug (maybe the same one)
        // rm test 2> /dev/null
        // Tokens list: [Token[type=STRING, val=rm], Token[type=STRING, val=test], Token[type=REDIRECT_ERR, val=2>], Token[type=STRING, val=/dev/null]]
        // Abstract Syntax Tree:
        // AST ROOT
        // └── COMMAND: [rm, test] | REDIRECTS: [RedirectOp[opType=REDIRECT_ERR, target=/dev/null]]
        // Executing: [rm, test]
        // shell: command not found: rm
        log.debug("Starting application");
        new Shell().start();
    }
}
