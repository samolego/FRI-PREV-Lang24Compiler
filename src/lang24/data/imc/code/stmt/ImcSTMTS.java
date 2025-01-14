package lang24.data.imc.code.stmt;

import lang24.common.logger.Logger;
import lang24.data.imc.visitor.AbstractImcVisitor;

import java.util.List;
import java.util.Vector;

/**
 * Sequence of statements.
 * 
 * Executes one statement after another.
 */
public class ImcSTMTS extends ImcStmt {

	/** The sequence of statements. */
	public final Vector<ImcStmt> stmts;

	/**
	 * Constructs a sequence of statements.
	 * 
	 * @param stmts The sequence of statements.
	 */
	public ImcSTMTS(List<ImcStmt> stmts) {
		this.stmts = new Vector<ImcStmt>(stmts);
	}

	@Override
	public <Result, Arg> Result accept(AbstractImcVisitor<Result, Arg> visitor, Arg accArg) {
		return visitor.visit(this, accArg);
	}

	@Override
	public void log(Logger logger) {
		logger.begElement("imc");
		logger.addAttribute("instruction", "STMTS");
		for (int s = 0; s < stmts.size(); s++)
			stmts.get(s).log(logger);
		logger.endElement();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("STMTS(");
		for (int s = 0; s < stmts.size(); s++) {
			if (s > 0)
				buffer.append(",");
			buffer.append(stmts.get(s).toString());
		}
		buffer.append(")");
		return buffer.toString();
	}

}