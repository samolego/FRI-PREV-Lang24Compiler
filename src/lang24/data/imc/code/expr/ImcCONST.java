package lang24.data.imc.code.expr;

import lang24.common.logger.*;
import lang24.data.imc.visitor.*;

/**
 * Constant.
 * 
 * Returns the value of a constant.
 */
public class ImcCONST extends ImcExpr {

	/** The value. */
	public final long value;

	/**
	 * Constructs a new constant.
	 * 
	 * @param value The value.
	 */
	public ImcCONST(long value) {
		this.value = value;
	}

	@Override
	public <Result, Arg> Result accept(AbstractImcVisitor<Result, Arg> visitor, Arg accArg) {
		return visitor.visit(this, accArg);
	}

	@Override
	public void log(Logger logger) {
		logger.begElement("imc");
		logger.addAttribute("instruction", toString());
		logger.endElement();
	}

	@Override
	public String toString() {
		return "CONST(" + value + ")";
	}

}