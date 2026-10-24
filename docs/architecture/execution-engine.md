# Process Builder

The ProcessBuilder is a mutable factory for OS processes.
I'm going to use it to sping new processes.

The ProcessBuilder is way more flexible than normal Runtime.exec()
as it allows to configure the standard I/O, environment variables,
and working directories - before handling the configuration over to the OS.

## The primary configs methods I found that I'm going to use:

* `ProcessBuilder command(String... command)`
  Defines the executable and its arguments.
  It accepts a list of varargs rather than a single string
  to avoid shell tokenization vulnerabilities and escaping shit.
  The JVM passes these exact tokens directly to the OS system call.
  - I'll use it to set the command the process builder will run (with the args).

* `ProcessBuilder directory(File directory)`
  Overrides the default working directory (which is inherited from the parent JVM).
  File operations inside the new process will resolve relative to this directory,
  ensuring the spawned process operates in isolation.
  - I'll use it to set the process directory to the current wd from the ShellContext.

* `Map<String, String> environment()`
  Returns a live, modifiable view of the environment variables.
  - I'll add the relevant variables from the ShellContext before it starts.

* `ProcessBuilder redirectInput(File file)`, `redirectOutput(...)`, `redirectError(...)`
  - I'll use these to redirect I/O based on the RedirectOps the command contains.

* `ProcessBuilder redirectErrorStream(boolean redirectErrorStream)`
  Instructs the OS to merge the child process's stderr stream directly into its stdout stream.
  This guarantees chronological interleaving of log messages
  and saves the JVM from needing to manage two separate reader threads to prevent buffer deadlocks.

  After calling `redirectErrorStream(true)`, any configuration made via `redirectError()` is ignored.
  The destination of the merged stream is controlled exclusively by `redirectOutput()`
  //todo Learn more about this

---

Once configuration is complete, I can invoke the factory method:
`--- Process start() ---`

This translates the Java configuration into a native OS system call,
allocating the OS resources, and returning a Process object to track it.

## Exceptions thrown:

* `IOException`:
  Thrown if the OS fails to create the process.
  This is typically happens if the executable does not exist,
  the user lacks execution permissions,
  or the working directory specified by directory() does not exist.

* `NullPointerException`:
  Thrown if the command list itself, or any specific element within it - is null.

* `IndexOutOfBoundsException`:
  Thrown if the command list is empty.

* `SecurityException`:
  Thrown if a SecurityManager is present and its checkExec method denies the execution.
  ** I don't know anything about security managers at the moment,
      but it doesn't seem something to worry about. **

---

## The Process Object Lifecycle:

When `ProcessBuilder.start()` succeeds, it returns a Process object.
This object is a proxy that allows the parent JVM to inspect,
wait for, or terminate the OS-level child process running concurrently.

Here are the critical lifecycle methods I found:

* `int waitFor()`:
  A blocking method that will halt the parent thread until the child process terminates,
  and return the exit code.

  Throws `InterruptedException`
  if another thread in the parent JVM interrupts the thread that is currently waiting.

  *Important*
  This exception means the parent's waiting thread was interrupted.
  NOT that the child OS process was killed.
  The child process continues running in the background even if this exception is thrown.

* `int exitValue()`
  A Non-Blocking method, returns the status code in that EXACT moment.

  Throws `IllegalThreadStateException`
  if this method called while the child process is still running.

* `void destroy()`
  Requests graceful termination.
  On Unix, this typically sends a SIGTERM signal.
  The child process can intercept this signal, run shutdown hooks, and exit cleanly.
  * The child process is also allows to ignore it and keep running. *

* `Process destroyForcibly()`
  On Unix, this sends a SIGKILL signal.
  The OS terminates the process immediately at the kernel level.
  The child process cannot intercept this, so the termination is guaranteed.

# Redirections

The redirection engine is responsible for overriding the standard I/O streams of a process before it starts running.

The shell recognizes several redirection signs, the Lexer detect them, and the parser assign them into RedirectOp objects attached to CmdAstNode.
A command can have multiple redirectors.

## The Symbols:

* `<` (REDIRECT_IN): Uses a file as STDIN.
* `>` OR `1>` (REDIRECT_OUT): Overwrites a file with STDOUT.
* `>>` OR `1>>` (APPEND_REDIRECT_OUT): Appends STDOUT to a file.
* `2>` (REDIRECT_ERR): Overwrites a file with STDERR.
* `2>>` (APPEND_REDIRECT_ERR): Appends STDERR to a file.
* `&>` (REDIRECT_BOTH): Overwrites a file with both STDOUT and STDERR.
* `&>>` (APPEND_REDIRECT_BOTH): Appends both STDOUT and STDERR to a file.
* `2>&1` (FD_MERGE_ERR_TO_OUT): Merges STDERR to STDOUT.
* `1>&2` (FD_MERGE_OUT_TO_ERR): Merges STDOUT to STDERR.

## The Pre-Flight Check (RedirectionResolver):

Before a command actually executes, the CmdAstNode passes the list of RedirectOp to the RedirectionResolver.
This is a critical security phase.

If I were to blindly pass the redirect ops to the OS via ProcessBuilder and the file requires root permissions, the process will fail *after* spawning, or even fail silently and corrupt streams.

The RedirectionResolver acts as a dry-run:

1. Iterates through the redirect ops list in order (later operators overwrites earlier ones).
2. Resolves the absolute path based on the ShellContext's working directory.
3. Validates filesystem rules (exists, readable, writable, has permissions...).
4. If any check fails → it throws a RedirectionException.

## The StreamState:

If the pre-flight check succeeds, the RedirectionResolver populates a StreamState object.
By default, StreamState points its inputs and outputs to the shell's current terminal stream.

If a redirection operator is applied, the StreamState stores a reference to the File and a boolean flag indicating if it should overwrite or append.

## The CommandExecutor:

Now the CommandExecutor takes over. It handles I/O differently depending on the command type:

* **Native OS Commands (ProcessBuilder):**
  Native commands run in a completely separate OS process. I don't read bytes manually; I instruct the kernel to wire the streams.

  The applyNativeRedirections() method translates the StreamState into ProcesBuilder.Redirect commands:
  - `ProcesBuilder.Redirect.INHERIT`: If the stream is untouched.
      The OS wires the child process directly to the JVM's terminal.
  - `ProcessBuilder.Redirect.to(File)`: For standard `>`.
  - `ProcessBuilder.Redirect.appendTo(File)`: For `>>`.

  If custom Virtual Threads are used for Pipes (`|`), the ProcessBuilder uses its default pipe mechanism, and my JVM pumps the bytes between the processes concurrently.

* **Shell Built-in Commands:**
  Built-in commands (like cd, echo, pwd) run *inside* the JVM. They do not use ProcessBuilder.
  Instead, the CommandExecutor translates the StreamState's File references into actual FileInputStream and FileOutputStream objects.

  These streams are passed directly into the ShellCommand.execute() method.

## The finally block (Resource Leaks):

For built-ins, the CommandExecutor must close the custom streams it opens.
A critical finally block ensures that if I created a FileOutputStream for a redirection, I can .close() on it.
If I inherited the standard terminal stream, I leave it open, otherwise closing it would completely brick the shell's ability to print further output.
