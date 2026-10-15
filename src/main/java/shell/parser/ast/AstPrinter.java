package shell.parser.ast;

public class AstPrinter {
    public static String getTreeStruct(AstNode root) {
        StringBuilder sb = new StringBuilder("AST ROOT\n");
        printNode(root, "", true, sb);
        return sb.toString();
    }

    private static void printNode(AstNode node, String prefix, boolean isTail, StringBuilder sb) {
        if (node == null) return;

        sb.append(prefix).append(isTail ? "└── " : "├── ");
        
        switch (node) {
            case CmdAstNode cmd -> {
                sb.append("COMMAND: ").append(cmd.args());
                if (!cmd.redirectors().isEmpty()) {
                    sb.append(" | REDIRECTS: ").append(cmd.redirectors());
                }
                sb.append("\n");
            }
            case PipeAstNode pipe -> {
                sb.append("PIPE\n");

                printNode(pipe.left(), prefix + (isTail ? "    " : "│   "), false, sb);
                printNode(pipe.right(), prefix + (isTail ? "    " : "│   "), true, sb);
            }
            case LogicalAstNode logical -> {
                sb.append("LOGICAL: ").append(logical.type()).append("\n");
                printNode(logical.left(), prefix + (isTail ? "    " : "│   "), false, sb);
                printNode(logical.right(), prefix + (isTail ? "    " : "│   "), true, sb);
            }
            case BgAstNode bg -> {
                sb.append("BACKGROUND JOB\n");
                printNode(bg.next(), prefix + (isTail ? "    " : "│   "), true, sb);
            }
            default -> sb.append("UNKNOWN NODE TYPE: ").append(node.getClass().getSimpleName()).append("\n");
        }
    }
}
