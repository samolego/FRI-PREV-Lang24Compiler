package lang24.common.report;

import lang24.data.ast.tree.AstNode;

public class ErrorAtBuilder {
    private final StringBuilder sb;

    public ErrorAtBuilder(final String messageTop, final AstNode location) {
        this.sb = new StringBuilder();

        sb.append("Error: \n");
        sb.append(messageTop);

        sb.append("\n");
        sb.append("     | ");
        sb.append("\n");
        this.addLocation(location);
        sb.append("\n");
    }

    public ErrorAtBuilder addString(String message) {
        sb.append(message);
        sb.append("\n");

        return this;
    }

    public ErrorAtBuilder addLocation(AstNode location) {
        var ln = String.format("%4d |    ", location.location().begLine);

        // Make error like this:
        // <line number> | <line text>
        //                 ^^^^^^^^^^^

        sb.append(ln);

        final String lineText = location.getText().split("\n")[0];
        sb.append(lineText);
        sb.append("\n");
        sb.append("     |    ");
        sb.append("^".repeat(1 + Math.max(0, location.location().endColumn - location.location().begColumn)));
        sb.append("\n");

        return this;
    }


    @Override
    public String toString() {
        return sb.toString();
    }
}
