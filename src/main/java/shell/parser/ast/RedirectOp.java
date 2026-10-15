package shell.parser.ast;

import shell.lexer.Lexer;

public record RedirectOp(Lexer.TokenType opType, String target) {
}
