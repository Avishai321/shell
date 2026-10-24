Alias expansion operates as a distinct preprocessing phase.
It sits between the `Lexer` and the `Parser`.
Because the POSIX standard requires aliases to be evaluated as text that is
subsequently tokenized, I cannot substitute aliases during execution.

## Identifying the "Command Word"
Aliases only expand if they are in the command word position.
I'm tracking this using expectingCommandWord flag.
* The flag initialized to `true`.
* It resets to `true` whenever a control operator is encountered.
* Redirections are completely bypassed 
* Once a command word is found, the flag flips to `false`, treating all
    subsequence strings as arguments until the next control operator.

## Infinite loop prevention (The Blackout Set)
Exists to prevent 'alias ls="ls --color=auto"' from causing a StackOverflowError.
1. When a token triggers expansion, its name is added to the blackout set for that specific recursion branch.
2. The alias value is re-lexed, and those new tokens are recursively passed back into the expander.
3. If the expander encounters a token that exists in the blackout set, it will add it as a literal string.
4. The blackout sets are cloned down the recursion tree, ensuring that a parallel command
    in a pipeline do not pollute blacklist aliases  for each other.