package shell.lexer;

import shell.core.ShellContext;
import shell.lexer.Lexer.Token;
import shell.lexer.Lexer.TokenType;

import java.util.*;

public class AliasExpander {
    public static List<Token> expand(
            List<Token> initialTokens,
            ShellContext context,
            Lexer lexer) {

        return expandRecursive(initialTokens, context, lexer, new HashSet<>());
    }

    private static List<Token> expandRecursive(
            List<Token> tokens,
            ShellContext context,
            Lexer lexer,
            Set<String> blackoutSet) {

        List<Token> result = new ArrayList<>();

        // Track if looking for a command word, true at the start of a pipeline.
        boolean expectingCommandWord = true;

        // True if bypassing a redirection target.
        // Example: in "< file.txt", "file.txt" is a target, not a command.
        boolean skipNextString = false;

        for (Token token : tokens) {
            TokenType type = token.type();

            // A control operator explicitly ends the current command context
            if (type.isControlOperator()) {
                expectingCommandWord = true;
                skipNextString = false;
                result.add(token);
                continue;
            }

            // The token that comes after a redirection is ALWAYS a file target.
            // It must be bypassed during the alias evaluation.
            if (type.isRedirection()) {
                skipNextString = true;
                result.add(token);
                continue;
            }

            // File descriptor merges do not have string targets after them.
            if (type.isFdMerge()) {
                result.add(token);
                continue;
            }

            if (type.isLiteral()) {
                if (skipNextString) {
                    // This string is the target of a previous redirection op.
                    skipNextString = false;
                    result.add(token);
                    continue;
                }

                if (expectingCommandWord) {
                    // This is a command word candidate.
                    expectingCommandWord = false;
                    String tokenVal = token.val();

                    Map<String, String> aliases = context.getAliases();
                    if (aliases.containsKey(tokenVal) && !blackoutSet.contains(tokenVal)) {
                        String aliasVal = aliases.get(tokenVal);

                        // Clone the blackout set so parallel aliases don't pollute each other.
                        Set<String> branchBlackout = new HashSet<>(blackoutSet);
                        branchBlackout.add(tokenVal);

                        // Tokenize the raw alias string into new tokens.
                        List<Token> aliasTokens = lexer.tokenize(aliasVal);

                        // Pass the new tokens back into the expander
                        // with the updated blackout set to resolve nested aliases.
                        List<Token> expandedTokens = expandRecursive(
                                aliasTokens,
                                context, lexer,
                                branchBlackout
                        );

                        // Inject the fully resolved tokens into the main stream.
                        result.addAll(expandedTokens);
                    } else {
                        result.add(token);
                    }
                } else {
                    // This string is an argument to an already established command.
                    result.add(token);
                }
            }
        }

        return result;
    }
}
