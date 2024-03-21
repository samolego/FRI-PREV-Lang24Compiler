package lang24.common.report;

public class TextLocation extends Location {
    private final String text;

    public TextLocation(String text, int begLine, int begColumn, int endLine, int endColumn) {
        super(begLine, begColumn, endLine, endColumn);
        this.text = text;
    }

    public TextLocation(String text, int line, int column) {
        super(line, column);
        this.text = text;
    }

    public TextLocation(String text, Locatable that) {
        super(that);
        this.text = text;
    }

    public TextLocation(String text, Locatable beg, Locatable end) {
        super(beg, end);
        this.text = text;
    }

    @Override
    public String getText() {
        return text;
    }
}
