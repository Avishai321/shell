# The Lexer

The Lexer's job is to read the raw input string char by char, and group them into 'Tokens'.
A Token holds it's category (TokenType) and it's raw string value.

The Lexer operates like a Finite State Machine to handle complexities of shell input.

## The States:

* **NORMAL:**
  This is the default state.
  Spaces acts as terminators or separators between tokens.
  Operators like `|`, `<` are intercepted and converted into specific tokens.

* **DOUBLE QUOTE:**
  In this mode, spaces and operators loses their meaning.
  Everything is treated as a raw string until the closing `"`.

* **SINGLE QUOTE:**
  Very similar to DOUBLE QUOTE but even less sensitive.
  It will ignore escape `\` and put them as they are.

* **ESCAPE:**
  Triggered by backslash `\`.
  The very next character is treated as a literal STRING,
  bypassing any special operator meaning.

When the Lexer encounters a special character like 1, it must do some checks.
1 could be a literal string, `1>` (stdout redirect), `1>>` (append), and `1>&2` (fd merge).
I match the longest possible sequence first, to prevent `1>&2` incorrectly treated as `1>` `&2`.

# The Parser

While the Lexer groups characters into Tokens, it doesn't care about the rules of the shell.
The Lexer will tokenize EVERYTHING even if it makes no sense.

The Parser consumes the linear list of Tokens and enforces shell rules, by converting the list into the Abstract Syntax Tree (AST).

The Parser uses a technique called Recursive Descent.
It evaluates operations from the lowest precedence to the highest.
Because lower precedence operations are evaluated first, they end up higher in the tree.

## The Precedence Hierarchy (Lowest to Highest):

1. Job Control (`&` - SEND_TO_BACKGROUND)
2. Logical Operators (`&&`, `||`)
3. Pipelines (`|`)
4. Commands & Redirections (STRING, `>`, `<`, ...)

The Parser outputs the root of the AST.
Every node implements an `execute()` method.
Execution starts at the Root and continue down to the leaves.

For example:
A Logical `&&` Node, will implement it's execute() method
as executing the left child first, and if it returns exit code == 0 ->
it will execute the right child.
