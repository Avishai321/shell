package shell.parser.ast;

import lombok.extern.slf4j.Slf4j;
import shell.core.ShellContext;
import shell.execution.CommandExecutor;
import shell.execution.RedirectionException;
import shell.execution.RedirectionResolver;
import shell.execution.StreamState;
import shell.util.Util;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

@Slf4j
public record CmdAstNode(List<String> args, List<RedirectOp> redirectors) implements AstNode {
    @Override
    public int execute(InputStream in, OutputStream out, OutputStream err, ShellContext context) {
        log.debug("Executing: {}", args);

        StreamState streamState = new StreamState(in, out, err);

        try {
            RedirectionResolver.resolve(redirectors, streamState, context);
        } catch (RedirectionException e) {
            Util.println(err, e.getMessage());
            return 1;
        }

        return CommandExecutor.executeCommand(args, streamState, context);
    }
}
