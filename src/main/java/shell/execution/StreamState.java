package shell.execution;

import lombok.RequiredArgsConstructor;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/*
Holds the final I/O state *after* all redirection operators are parsed.
 */
@RequiredArgsConstructor
public class StreamState {
    public final InputStream terminalIn;
    public final OutputStream terminalOut;
    public final OutputStream terminalErr;

    // These will be null if the user didn't redirect them,
    // meaning they should inherit the terminal streams above.
    public File redirectIn = null;
    public File redirectOut = null;
    public boolean appendOut = false;
    public File redirectErr = null;
    public boolean appendErr = false;
}
