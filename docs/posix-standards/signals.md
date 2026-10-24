# POSIX Signals

## The problem:

When a user presses Ctrl+C in a terminal, the OS sends a SIGINT (interrupt) signal to the foreground process.
By default, the JVM intercepts this signal and initiates a graceful shutdown,
triggering any registered shutdown hook before terminating the process.

That means that without handling it, pressing Ctrl+C in my shell will terminate the shell itself as well.
I don't want that, Ctrl+C should interrupt the current running command, not to kill the entire shell.

There is the `sun.misc.Signal` API that allows to override SIGINT default behaviors.
Here is how it works:

**sun.misc.Signal**
An object representing a specific OS signal.
I can instantiate it with the signal's standard POSIX string name (without the "SIG" prefix).
    such as "INT", "TERM" or "HUP".

**sun.misc.SignalHandler**
An interface with a single `handle(Signal sig)` method.
It dictates what the JVM should do when the specified signal is received.

The SignalHandler interface provides two static,
pre-defined handlers mapping directly to standard OS-level signal dispositions:

1. `SignalHandler.SIGN_IGN` (Ignore):
    Instruct the JVM to completely discard the signal.
2. `SignalHandler.SIG_DFL` (Default):
    Restores the JVM's default signal handler.
    For SIGINT, this means restoring the JVM shutdown sequence.

Here is how I can ignore the INT signal:
`Signal.handle(new signal("INT", SignalHandler.SIG_IGN);`

## UPDATE:

I just discovered some problems with Signal
1. This is deprecated, it may be removed in a future release.

//todo list the shell prompt corruption,
    the point that this should interrupt the main thread's loop
    and make it start a new cycle instead of printing the prompt manually.

//todo Here talk about the JLine library and how it solves my problems.
