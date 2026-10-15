package shell.lexer;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
    public enum TokenType {
        // IMPORTANT: ordered from the lowest precedence to the highest.
        SEND_TO_BACKGROUND,
        AND,
        OR,
        PIPE,

        REDIRECT_IN,
        REDIRECT_OUT,
        APPEND_REDIRECT_OUT,
        REDIRECT_ERR,
        APPEND_REDIRECT_ERR,

        STRING;

        public boolean isRedirection() {
            return this == REDIRECT_IN
                    || this == REDIRECT_OUT
                    || this == APPEND_REDIRECT_OUT
                    || this == REDIRECT_ERR
                    || this == APPEND_REDIRECT_ERR;
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
                        case '<' -> {
                            if (!sb.isEmpty()) tokens.add(createStringToken(sb));
                            tokens.add(new Token(TokenType.REDIRECT_IN, "<"));
                        }
                        case '|' -> i = processOperator(
                                input, i, '|',
                                TokenType.PIPE, TokenType.OR,
                                sb, tokens
                        );
                        case '&' -> i = processOperator(
                                input, i, '&',
                                TokenType.SEND_TO_BACKGROUND, TokenType.AND,
                                sb, tokens
                        );
                        case '>' -> i = processOperator(
                                input, i, '>',
                                TokenType.REDIRECT_OUT, TokenType.APPEND_REDIRECT_OUT,
                                sb, tokens
                        );
                        case '2' -> {
                            if (i + 1 < input.length() && input.charAt(i + 1) == '>') {
                                if (!sb.isEmpty()) tokens.add(createStringToken(sb));

                                if (i + 2 < input.length() && input.charAt(i + 2) == '>') {
                                    tokens.add(new Token(TokenType.APPEND_REDIRECT_ERR, "2>>"));
                                    i += 2;
                                } else {
                                    tokens.add(new Token(TokenType.REDIRECT_ERR, "2>"));
                                    i++;
                                }
                            } else sb.append(c);
                        }
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

    private static int processOperator(
            String input,
            int index,
            char opChar,
            TokenType singleType,
            TokenType doubleType,
            StringBuilder sb,
            List<Token> tokens
    ) {
        if (!sb.isEmpty()) tokens.add(createStringToken(sb));

        if (index + 1 < input.length() && input.charAt(index + 1) == opChar) {
            tokens.add(new Token(doubleType, String.valueOf(opChar) + opChar));
            return index + 1;
        }

        tokens.add(new Token(singleType, String.valueOf(opChar)));
        return index;
    }
}
