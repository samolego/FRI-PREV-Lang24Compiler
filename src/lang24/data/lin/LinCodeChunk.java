package lang24.data.lin;

import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.mem.MemFrame;
import lang24.data.mem.MemLabel;

import java.util.List;
import java.util.Vector;

/**
 * A chuck of code.
 * @param frame  A frame of a function. 
 * @param stmts  The statements of a function body. 
 * @param entryLabel
The function's body entry label, i.e., the label the prologue jumps to.
 * @param exitLabel
The function's body exit label, i.e., the label at which the epilogue starts.
 */
public record LinCodeChunk(MemFrame frame, Vector<ImcStmt> stmts, MemLabel entryLabel, MemLabel exitLabel) {

	/**
	 * Constructs a new code chunk.
	 *
	 * @param frame      A frame of a function.
	 * @param stmts      The statements of a function body.
	 * @param entryLabel The function's body entry label, i.e., the label the
	 *                   prologue jumps to.
	 * @param exitLabel  The function's body exit label, i.e., the label at which
	 *                   the epilogue starts.
	 */
	public LinCodeChunk(MemFrame frame, List<ImcStmt> stmts, MemLabel entryLabel, MemLabel exitLabel) {
        this(frame, new Vector<>(stmts), entryLabel, exitLabel);
    }

	/**
	 * Returns the statements of a function body.
	 *
	 * @return The statements of a function body.
	 */
	@Override
	public Vector<ImcStmt> stmts() {
		return new Vector<>(stmts);
	}

}
