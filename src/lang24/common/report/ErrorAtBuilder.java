package lang24.common.report;

import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.tree.stmt.AstStmt;

/**
 * Error builder that can add source code lines and underlines to the error message.
 */
public class ErrorAtBuilder {
    private final StringBuilder sb;

    public ErrorAtBuilder(final String messageTop, final AstNode location) {
        this(messageTop);
        this.addUnderlinedSourceNode(location);
        sb.append("\n");
    }


    public ErrorAtBuilder(final String type, final String messageTop) {
        this.sb = new StringBuilder();

        sb.append(type);
        sb.append(": \n");
        sb.append(messageTop);

        sb.append("\n");
    }

    public ErrorAtBuilder(final String messageTop) {
        this("Error", messageTop);
    }


    /**
     * Adds an unformatted line to the error message.
     * @param message The message to add.
     * @return The builder.
     */
    public ErrorAtBuilder addLine(String message) {
        sb.append(message);
        sb.append("\n");

        return this;
    }

    /**
     * Adds a blank source line to the error message.
     * @return
     */
    public ErrorAtBuilder addBlankSourceLine() {
        sb.append("     |\n");
        return this;
    }

    /**
     * Adds a line from source code to the error message, underlined with carets.
     * Whole line is added, not just the node text.
     * @param node The node to underline.
     * @return
     */
    public ErrorAtBuilder addUnderlinedSourceNode(AstNode node) {
        this.addSourceLine(node);
        this.addOffsetedSquiglyLines(node, "");

        return this;
    }

    /**
     * Adds carets under the node with an offset. based on the column indentation.
     * @param node The node to underline.
     * @param message The message to add under the carets.
     * @return
     */
    public ErrorAtBuilder addOffsetedSquiglyLines(AstNode node, String message) {
        var parent = findStatementNode(node).location();
        int offset = node.location().begColumn - parent.begColumn;
        int lineOffset = node.location().begLine - parent.begLine;

        if (offset < 0) {
            offset = 0;
        }

        return addSquiglyLines(node, offset, lineOffset, message);
    }

    /**
     * Adds carets under the node with the specified offset.
     * @param location The location to underline.
     * @param colOffset The column offset.
     * @param lineOffset The line offset.
     * @param message The message to add under the carets.
     * @return
     */
    public ErrorAtBuilder addSquiglyLines(Locatable location, int colOffset, int lineOffset, String message) {
        var ln = "     |    ";
        sb.append(ln);
        sb.append(" ".repeat(colOffset));

        int caretRepeat = 1 + Math.max(0, location.location().endColumn - location.location().begColumn);
        sb.append("^".repeat(caretRepeat));

        if (message != null && !message.isEmpty()) {
            sb.append("\n");
            sb.append(ln);
            sb.append(" ".repeat(colOffset));

            sb.append(message);
        }
        sb.append("\n");

        return this;
    }

    /**
     * Adds a source code line to the error message, formatted with line number.
     * Whole line is added, not just the node text.
     * @param node
     * @return
     */
    public ErrorAtBuilder addSourceLine(AstNode node) {
        var parent = findStatementNode(node);
        int lineOffset = node.location().begLine - parent.location().begLine;
        var ln = String.format("%4d |    ", node.location().begLine);

        // Make error like this:
        // <line number> | <line text>
        //                 ^^^^^^^^^^^

        sb.append(ln);
        var perLines =  parent.getText().split("\n");
        final String lineText = perLines[lineOffset];
        sb.append(lineText);
        sb.append("\n");

        return this;
    }

    /**
     * Adds a source code line to the error message, formatted with line number.
     * @param location The location of the line.
     * @param lineText The text of the line to add.
     * @return
     */
    public ErrorAtBuilder addSourceLine(Location location, String lineText) {
        var ln = String.format("%4d |    ", location.location().begLine);

        // Make error like this:
        // <line number> | <line text>
        //                 ^^^^^^^^^^^

        sb.append(ln);
        sb.append(lineText);
        sb.append("\n");

        return this;
    }

    /**
     * Adds the end of a source code line to the error message.
     * Usually '}'.
     * @param node The node to get the end line from.
     * @return
     */
    public ErrorAtBuilder addSourceLineEnd(AstNode node) {
        node = findStatementNode(node);
        var ln = String.format("%4d |    ", node.location().endLine);

        // Make error like this:
        // <line number> | <line text>
        //                 ^^^^^^^^^^^

        sb.append(ln);
        var lineTexts = node.getText().split("\n");
        final String lineText = lineTexts[lineTexts.length - 1];
        sb.append(lineText);
        sb.append("\n");

        return this;
    }


    /**
     * Finds the statement node from the given node. Usually the statement node is the relevant line from the source code.
     * @param node The node to find the statement node from.
     * @return The statement node.
     */
    private AstNode findStatementNode(AstNode node) {
        // The last part is root node check
        if (node instanceof AstStmt || node.parent == null || node.parent instanceof AstNodes<?> && node.parent.parent == null) {
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
