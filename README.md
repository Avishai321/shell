# Shell

### Project Structure
```text
src/main/java/shell/
    commands/  # Built-in command implementations.
    config/    # Config Utility Class.
    core/      # Shell (main entry point) with REPL loop, ShellContext.
    doc/       # TXT files explaining important/complex things.
    lexer/     # FSM Lexer, Token records, TokenType enum.
    parser/    # Recursive Descent parser, AstNode interface and node implementations.
    util/      # Util class with useful methods.
    Main.java
```

#### The doc directory
I'm recording my journey in these files including new things I learn while working on this project.  
It's not formatted as it should be, but it shows my development and some interesting things I found.  

#### Test & Run The Application:
* cd **project path**
* mvn clean compile
* mvn exec:java -Dexec.mainClass="shell.Main"
* **debug mode:** mvn exec:java -Dexec.mainClass="shell.Main" -DlogLevel=DEBUG
