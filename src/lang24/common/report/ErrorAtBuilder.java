package lang24.common.report;

public class ErrorAtBuilder {
    private final StringBuilder sb;

    public ErrorAtBuilder(final String messageTop, final Locatable location) {
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

    public ErrorAtBuilder addLocation(Locatable location1) {
        var ln = String.format("%4d |    ", location1.location().begLine);

        // Make error like this:
        // <line number> | <line text>
        //                 ^^^^^^^^^^^

        sb.append(ln);

        final String lineText = location1.getText().replaceAll("\n", "\n     |    ");
        sb.append(lineText);
        sb.append("\n");
        sb.append("     |    ");
        sb.append(" ".repeat(Math.max(0, location1.location().begColumn - 11)));
        sb.append("^".repeat(1 + Math.max(0, location1.location().endColumn - location1.location().begColumn)));
        sb.append("\n");

        return this;
    }


    @Override
    public String toString() {
        return sb.toString();
    }
}
