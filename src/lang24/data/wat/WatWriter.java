package lang24.data.wat;

import java.io.PrintWriter;

public class WatWriter implements AutoCloseable{
    private final PrintWriter writer;
    private int indentLevel = 0;

    public WatWriter(PrintWriter writer) {
        this.writer = writer;
    }

    public void indent() { indentLevel++; }
    public void unindent() { indentLevel--; }

    /**
     * Prints a line with current indentation.
     */
    public void println(String format, Object... args) {
        String text = String.format(format, args);
        writer.println("  ".repeat(indentLevel) + text);
    }

    /**
     * Opens a Wasm group: Prints "(label", then increases indentation.
     */
    public void groupStart(String format, Object... args) {
        println(format, args);
        indent();
    }

    /**
     * Closes a Wasm group: Decreases indentation, then prints ")".
     */
    public void groupEnd() {
        unindent();
        println(")");
    }

    @Override
    public void close() {
        this.writer.close();
    }
}
