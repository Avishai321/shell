# The Shell

The shell is the software that acts as the bridge between the user and the OS's kernel.

The shell's lifecycle, "REPL" (Read, Eval, Print, Loop)

* **Read (The Prompt):**
  The shell prints a prompt, (like `&` or `>`),
  usually with the path of the current working directory
  and waits for the user to commit a command.
  It captures the string of text after the user hits Enter.
  It also listens for system signals, like Ctrl+C to interrupt a process.

* **Tokenization (Lexing):**
  The shell takes the string and chops it into "tokens".
  It handles quotes, escaping and spaces.

* **Parsing (Syntax Analysis):**
  The shell analyzes the tokens to understand the structure.
  It what every token means (e.g. `|` means piping data between two processes).
  It builds an Abstract Syntax Tree (AST) - a data structure representing the logical execution order.

* **Evaluation & Execution:**
  The shell decides what to do with the input.
  If its blank → there is not reason to do anything, just continue.

    1. Take the input and use the Lexer to tokenize it.
       Now I'm holding a list of tokens and not just text.

    2. Pass the tokens list to the Parser to build the AST (see parser.Parser for more info).
       The Parser will return the root of the AST which can be executed.

    3. Call root.execute() to start the execution of the command.
       this will also return the exit code.

* **Print:**
  The AST nodes will do the printing.
  So in my case I shouldn't worry about this in the REPL level.

* **Loop:**
  The shell resets its state, prints the prompt again, and waits for the next command.

## Starting the shell:

1. Initialize a new ShellContext (see more in "doc/Shell Context")
2. Start the REPL Loop

The shell also tracks the lastExitCode & lastExecTime

# Shell Context

The Shell Context is what holds important things the shell needs to operate.

It's a pretty small file, that contains very important things:

### `volatile boolean isRunning`

This is what keeps the shell's loop alive.
Everything can continue only as long as this is set to true.
The exit command can change it to false which will cause the loop break.

This is volatile because it can be changed and read from different thread.

### `volatile int shellExitCode`

This is the code the shell will exit with when calling 'exit'.
For now, there is no important reason to make it volatile,
but I left it this way because I'm afraid it's usage is going to change in the future.

### `Path wd`

This is the shell's current working directory.
The cd command can change this.

It's being used in the shell prompt itself
and I also pass it to every new process as it's directory (See doc/Process Builder)

Initially set to the home directory.

### `final Map<String, ShellCommand> shellCommands`

Contains command.shellCommand instances.
Here I store all the shell's native commands such as cd, exit, pwd, etc.

When executing commands, I first check if it exists in this map,
and if it is → I'm calling its .execute() method and skipping
the new ProcessBuilder spinning.

### 'final Map<String, String> aliases'

A map of aliases and their values.

* Scope: Internal FSM text-replacement macros.
* Inheritance: Exist only during the lexical analysis phase.
  They are never passed to external OS processes, nor accessible as standard vars.
* POSIX Trigger: Created using `alias name="value"`.
* Subshell Behavior: Copied to subshells for lexical evaluation of grouped commands,
  but isolated from the parent.

### `final Map<String, String> exportedEnvVar`

Because System.getenv() is immutable,
My initialization logic cannot simply hold a reference to it.
I must instantiate a new, mutable data structure (this Map),
and copy System.getenv() entries into it.

* Scope: Global to the shell and all external child OS processes.
* Inheritance: When spinning up an external binary via ProcessBuilder,
  these vars must be injected into pb.environment().
* POSIX Trigger: Created using `export VAR="value"`.
* Subshell Behavior: Copied to subshells and inherited by any native OS process spawned by that subshell.

### `final Map<String, String> localEnvVars`

* Scope: Strictly internal to the Java shell process.
  These are used for the shell scripting logic, loops, and temporary state.
* Inheritance: They are never passed to ProcessBuilder.
  External native binaries (like ls or grep) cannot see them.
* POSIX Trigger: Created using `VAR="value"` (without the export keyword).
* Subshell Behavior; Copied to subshells (because a subshell executes within my Java architecture),
  but completely invisible to external OS processes.
