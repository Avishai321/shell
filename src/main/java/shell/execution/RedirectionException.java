package shell.execution;

/*
This is my way to abort execution if a pre-flight check fails
without returning integer error codes inside a deep loop.
 */
public class RedirectionException extends Exception {
    public RedirectionException(String msg) {
        super(msg);
    }
}
