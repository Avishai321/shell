package shell.lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    public enum TokenType {
        // IMPORTANT: ordered from the lowest precedence to the highest.
        SEND_TO_BACKGROUND, AND, OR, PIPE,

        REDIRECT_IN, REDIRECT_OUT, APPEND_REDIRECT_OUT,
        REDIRECT_ERR, APPEND_REDIRECT_ERR,
        REDIRECT_BOTH, APPEND_REDIRECT_BOTH,
        FD_MERGE_ERR_TO_OUT, FD_MERGE_OUT_TO_ERR,

        STRING;

        public boolean isCommandToken() {
            return this == STRING || isRedirection() || isFdMerge();
        }

        public boolean isRedirection() {
            return this == REDIRECT_IN || this == REDIRECT_OUT
                    || this == APPEND_REDIRECT_OUT
                    || this == REDIRECT_ERR || this == APPEND_REDIRECT_ERR
                    || this == REDIRECT_BOTH || this == APPEND_REDIRECT_BOTH;
        }

        public boolean isFdMerge() {
            return this == FD_MERGE_ERR_TO_OUT || this == FD_MERGE_OUT_TO_ERR;
        }
    }

    public record Token(TokenType type, String val) {
    }

    private enum STATE {
        NORMAL,
        DOUBLE_QUOTE,
        SINGLE_QUOTE,
        ESCAPE
    }

    public List<Token> tokenize(String input) {
        List<Token> tokens = new ArrayList<>();

        STATE state = STATE.NORMAL;
        STATE prevState = STATE.NORMAL;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            switch (state) {
                case NORMAL -> {
                    switch (c) {
                        case ' ' -> {
                            if (!sb.isEmpty()) tokens.add(createStringToken(sb));
                        }
                        case '<' -> i = processOperator(
                                input, i, c, sb, tokens,
                                "<", TokenType.REDIRECT_IN
                        );
                        case '>' -> i = processOperator(
                                input, i, c, sb, tokens,
                                ">>", TokenType.APPEND_REDIRECT_OUT,
                                ">", TokenType.REDIRECT_OUT
                        );
                        case '|' -> i = processOperator(
                                input, i, c, sb, tokens,
                                "||", TokenType.OR,
                                "|", TokenType.PIPE
                        );
                        case '&' -> i = processOperator(
                                input, i, c, sb, tokens,
                                "&>>", TokenType.APPEND_REDIRECT_BOTH,
                                "&>", TokenType.REDIRECT_BOTH,
                                "&&", TokenType.AND,
                                "&", TokenType.SEND_TO_BACKGROUND
                        );
                        case '1' -> i = processOperator(
                                input, i, c, sb, tokens,
                                "1>&2", TokenType.FD_MERGE_OUT_TO_ERR,
                                "1>>", TokenType.APPEND_REDIRECT_OUT,
                                "1>", TokenType.REDIRECT_OUT
                        );
                        case '2' -> i = processOperator(
                                input, i, c, sb, tokens,
                                "2>&1", TokenType.FD_MERGE_ERR_TO_OUT,
                                "2>>", TokenType.APPEND_REDIRECT_ERR,
                                "2>", TokenType.REDIRECT_ERR
                        );
                        case '"' -> state = STATE.DOUBLE_QUOTE;
                        case '\'' -> state = STATE.SINGLE_QUOTE;
                        case '\\' -> {
                            prevState = state;
                            state = STATE.ESCAPE;
                        }
                        default -> sb.append(c);
                    }
                }
                case DOUBLE_QUOTE -> {
                    if (c == '\\') {
                        prevState = state;
                        state = STATE.ESCAPE;
                    } else if (c == '"') state = STATE.NORMAL;
                    else sb.append(c);
                }
                case SINGLE_QUOTE -> {
                    if (c == '\'') state = STATE.NORMAL;
                    else sb.append(c);
                }
                case ESCAPE -> {
                    sb.append(c);
                    state = prevState;
                }
            }
        }

        if (!sb.toString().isBlank()) {
            tokens.add(createStringToken(sb));
        }

        return tokens;
    }

    private static Token createStringToken(StringBuilder sb) {
        String val = sb.toString();
        sb.setLength(0);
        return new Token(TokenType.STRING, val);
    }

    /*
    * Takes a list of objects in this format:
    *   String: "specific match", TokenType: type target.
    * The list must have the most specific matches first!.
    * Returns the index the calling loop should continue from.
     */
    private static int processOperator(
            String input,
            int index,
            char defaultChar,
            StringBuilder sb,
            List<Token> tokens,
            Object... mappings
    ) {
        for (int j = 0; j < mappings.length; j += 2) {
            String op = (String) mappings[j];
            TokenType type = (TokenType) mappings[j + 1];

            if (input.startsWith(op, index)) {
                if (!sb.isEmpty()) tokens.add(createStringToken(sb));
                tokens.add(new Token(type, op));
                return index + op.length() - 1;
            }
        }

        sb.append(defaultChar);
        return index;
    }
}
