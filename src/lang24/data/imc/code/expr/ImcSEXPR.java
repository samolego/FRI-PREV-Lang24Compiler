package lang24.data.imc.code.expr;

import lang24.common.logger.*;
import lang24.data.imc.code.stmt.*;
import lang24.data.imc.visitor.*;

/**
 * Statement expression.
 * 
 * Executes the statement, evaluates the expression and returns its value.
 */
public class ImcSEXPR extends ImcExpr {

	/** The statement. */
	public final ImcStmt stmt;

	/** The expression. */
	public final ImcExpr expr;

	/**
	 * Constructs a statement expression.
	 * 
	 * @param stmt The statement.
	 * @param expr The expression.
	 */
	public ImcSEXPR(ImcStmt stmt, ImcExpr expr) {
		this.stmt = stmt;
		this.expr = expr;
	}

	@Override
	public <Result, Arg> Result accept(AbstractImcVisitor<Result, Arg> visitor, Arg accArg) {
		return visitor.visit(this, accArg);
	}

	@Override
	public void log(Logger logger) {
		logger.begElement("imc");
		logger.addAttribute("instruction", "SEXPR");
		stmt.log(logger);
		expr.log(logger);
		logger.endElement();
	}

	@Override
	public String toString() {
		return "SEXPR(" + stmt.toString() + "," + expr.toString() + ")";
	}

}