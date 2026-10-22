package shell.core;

import lombok.extern.slf4j.Slf4j;
import shell.lexer.Lexer;
import shell.parser.Parser;
import shell.parser.ast.AstNode;
import shell.parser.ast.AstPrinter;
import shell.ui.PromptBuilder;
import sun.misc.Signal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Slf4j
public class Shell {
    private final ShellContext context;
    private final BufferedReader reader;
    private final Lexer lexer;
    private final Parser parser;

    private int lastExitCode = 0;
    private Duration lastExecTime = Duration.ZERO;

    public Shell() {
        this.context = new ShellContext();
        this.reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        this.lexer = new Lexer();
        this.parser = new Parser();
        log.debug("Shell helpers initialized.");

        //todo: I should disable icanon mode in order to implement advanced functionalities like
        // auto completion and shit.
        // Add documentation about the icanon mode, copy and modify it from my typing-speed-cli project.

        //todo: there is something that needs to be resolved before executing the program
        // mvn gives errors, here is one:
        //  [WARNING] /home/avishai/development/Java/Shell/src/main/java/shell/core/Shell.java:[3,16]
        //  sun.misc.Signal is internal proprietary API and may be removed in a future release
        Signal.handle(
                new Signal("INT"),
                sig -> {
                    //todo this should interrupt the repl loop instead.
                    // consider using Jline (update documentation if so.)
                    // Right now, Ctrl+C in the middle of operation will corrupt the next prompt.
                    System.out.print("\n" + PromptBuilder.getShellPrompt(1, Duration.ofNanos(0), context));
                    System.out.flush();
                }
        );
        log.debug("SIGINT handler configured.");
    }

    public void start() {
        log.debug("Shell is ready");

        while (context.isRunning()) {
            System.out.print(PromptBuilder.getShellPrompt(lastExitCode, lastExecTime, context));
            try {
                String input = this.reader.readLine();

                if (input.isBlank()) {
                    // It seems that zsh doesn't change the lastExitCode.
                    // So this.lastExitCode = 0; is intentionally not used here.
                    this.lastExecTime = Duration.ZERO;
                    continue;
                }

                List<Lexer.Token> tokens = lexer.tokenize(input);
                log.debug("Tokens list: {}", tokens);

                AstNode root = parser.createAst(tokens);
                log.debug("Abstract Syntax Tree:\n{}", AstPrinter.getTreeStruct(root));

                long startNano = System.nanoTime();
                this.lastExitCode = root.execute(System.in, System.out, System.err, context);
                long endNano = System.nanoTime();

                this.lastExecTime = Duration.ofNanos(endNano - startNano);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        System.exit(0);
    }
}
