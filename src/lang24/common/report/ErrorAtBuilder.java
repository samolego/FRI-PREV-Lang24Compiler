package lang24.common.report;

import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.tree.stmt.AstStmt;

public class ErrorAtBuilder {
    private final StringBuilder sb;

    public ErrorAtBuilder(final String messageTop, final AstNode location) {
        this(messageTop);
        this.addUnderlinedSourceNode(location);
        sb.append("\n");
    }


    public ErrorAtBuilder(final String messageTop) {
        this.sb = new StringBuilder();

        sb.append("Error: \n");
        sb.append(messageTop);

        sb.append("\n");
    }

    public ErrorAtBuilder addLine(String message) {
        sb.append(message);
        sb.append("\n");

        return this;
    }

    public ErrorAtBuilder addBlankSourceLine() {
        sb.append("     |\n");
        return this;
    }

    public ErrorAtBuilder addUnderlinedSourceNode(AstNode node) {
        this.addSourceLine(node);
        this.addOffsetedSquiglyLines(node, "");

        return this;
    }

    public ErrorAtBuilder addOffsetedSquiglyLines(AstNode node, String message) {
        int offset = node.location().begColumn - findStatementNode(node).location().begColumn;

        if (offset < 0) {
            offset = 0;
        }

        return addSquiglyLines(node, offset, message);
    }

    public ErrorAtBuilder addSquiglyLines(Locatable location, int offset, String message) {
        var ln = "     |    ";
        sb.append(ln);
        sb.append(" ".repeat(offset));

        int caretRepeat = 1 + Math.max(0, location.location().endColumn - location.location().begColumn);
        sb.append("^".repeat(caretRepeat));

        if (message != null && !message.isEmpty()) {
            sb.append("\n");
            sb.append(ln);
            sb.append(" ".repeat(offset));

            sb.append(message);
        }
        sb.append("\n");

        return this;
    }

    public ErrorAtBuilder addSourceLine(AstNode node) {
        node = findStatementNode(node);
        var ln = String.format("%4d |    ", node.location().begLine);

        // Make error like this:
        // <line number> | <line text>
        //                 ^^^^^^^^^^^

        sb.append(ln);
        final String lineText = node.getText().split("\n")[0];
        sb.append(lineText);
        sb.append("\n");

        return this;
    }


    private AstNode findStatementNode(AstNode node) {
        if (node instanceof AstStmt || node.parent == null || node.parent instanceof AstNodes<?>) {
            return node;
        } else {
            return findStatementNode(node.parent);
        }
    }


    @Override
    public String toString() {
        return sb.toString();
    }
}
