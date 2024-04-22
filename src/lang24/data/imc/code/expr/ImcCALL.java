package lang24.data.imc.code.expr;

import lang24.common.logger.Logger;
import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.mem.MemLabel;

import java.util.List;
import java.util.Vector;

/**
 * Function call.
 * 
 * Evaluates arguments (the static link must be included) from left to right,
 * calls the function denoted by the label provided and returns the function's
 * result.
 */
public class ImcCALL extends ImcExpr {

	/** The label of the function. */
	public final MemLabel label;

	/** The offsets of arguments. */
	public final Vector<Long> offs;

	/** The values of arguments. */
	public final Vector<ImcExpr> args;

	/**
	 * Constructs a function call.
	 * 
	 * @param label The label of the function.
	 * @param offs  The offsets of arguments.
	 * @param args  The values of arguments.
	 */
	public ImcCALL(MemLabel label, List<Long> offs, List<ImcExpr> args) {
		this.label = label;
		this.offs = new Vector<>(offs);
		this.args = new Vector<>(args);
	}

	@Override
	public <Result, Arg> Result accept(ImcVisitor<Result, Arg> visitor, Arg accArg) {
		return visitor.visit(this, accArg);
	}

	@Override
	public void log(Logger logger) {
		logger.begElement("imc");
		logger.addAttribute("instruction", "CALL(" + label.name + ")");
		for (int a = 0; a < args.size(); a++)
			args.get(a).log(logger);
		logger.endElement();
	}

	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("CALL(");
		buffer.append(label.name);
		for (int a = 0; a < args.size(); a++) {
			buffer.append(",");
			buffer.append(offs.get(a).toString());
			buffer.append(":");
			buffer.append(args.get(a).toString());
		}
		buffer.append(")");
		return buffer.toString();
	}

}