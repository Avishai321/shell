package shell.util;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.OutputStream;

@UtilityClass
public final class Util {
    public static int print(OutputStream out, String msg) {
        try {
            out.write(msg.getBytes());
            return 0;
        } catch (IOException e) {
            return 1;
        }
    }

    public static int println(OutputStream out, String msg) {
        return print(out, msg + "\n");
    }
}
