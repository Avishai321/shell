package shell.parser;

import shell.lexer.Lexer.Token;
import shell.parser.ast.*;

import java.util.ArrayList;
import java.util.List;

import static shell.lexer.Lexer.TokenType.*;

/*
Tokens with lower precedence should be higher on the tree.
    Precedence of operators:
    1. Job Control: SEND_TO_BACKGROUND
    2. Logical Operators: AND, OR
    3. Pipelines: PIPE
    4. Commands & Redirections: STRING, REDIRECT_IN, REDIRECT_OUT, APPEND_REDIRECT_OUT

    example: ls > test.txt && cat test.txt | grep "test" || echo "Failed"
    AST ROOT
    └── LOGICAL: OR
        ├── LOGICAL: AND
        │   ├── COMMAND: [ls] | REDIRECTS: [RedirectOp[opType=REDIRECT_OUT, target=test.txt]]
        │   └── PIPE
        │       ├── COMMAND: [cat, test.txt]
        │       └── COMMAND: [grep, test]
        └── COMMAND: [echo, Failed]
*/
public class Parser {
    //todo add documentation
    private List<Token> tokens;
    private int index;

    private Token peek() {
        return tokens.get(index);
    }

    private Token consume() {
        return tokens.get(index++);
    }

    private boolean hasNext() {
        return index < tokens.size();
    }

    public synchronized AstNode createAst(List<Token> tokens) {
        this.tokens = tokens;
        this.index = 0;

        AstNode root = parseLogical();
        if (hasNext() && peek().type() == SEND_TO_BACKGROUND) {
            consume();
            return new BgAstNode(root);
        }
        return root;
    }

    private AstNode parseLogical() {
        AstNode left = parsePipeLine();

        while (hasNext() && (peek().type() == OR || peek().type() == AND)) {
            Token token = consume();
            AstNode right = parsePipeLine();
            left = new LogicalAstNode(token.type(), left, right);
        }

        return left;
    }

    private AstNode parsePipeLine() {
        AstNode left = parseCommand();

        while (hasNext() && peek().type() == PIPE) {
            consume();
            AstNode right = parseCommand();
            left = new PipeAstNode(left, right);
        }

        return left;
    }

    private AstNode parseCommand() {
        List<String> args = new ArrayList<>();
        List<RedirectOp> redirectors = new ArrayList<>();

        while (hasNext() && (peek().type() == STRING || isRedirection(peek()))) {
            Token token = consume();

            if (token.type() == STRING) {
                args.add(token.val());
            } else if (isRedirection(token)) {
                Token target = consume();
                redirectors.add(new RedirectOp(token.type(), target.val()));
            }
        }

        return new CmdAstNode(args, redirectors);
    }

    private boolean isRedirection(Token token) {
        return token.type() == REDIRECT_IN
                || token.type() == REDIRECT_OUT
                || token.type() == APPEND_REDIRECT_OUT
                || token.type() == REDIRECT_ERR
                || token.type() == APPEND_REDIRECT_ERR;
    }
}
