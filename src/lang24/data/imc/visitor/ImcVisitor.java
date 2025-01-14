package lang24.data.imc.visitor;

import lang24.common.report.*;
import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.*;

/**
 * An abstract visitor of the intermediate code.
 * 
 * @author sliva
 *
 * @param <Result> The result the visitor produces.
 * @param <Arg>    The argument the visitor carries around.
 */
public interface ImcVisitor<Result, Arg> extends AbstractImcVisitor<Result, Arg> {

	@Override
	public default Result visit(ImcBINOP binOp, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcCALL call, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcCJUMP cjump, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcCONST constant, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcESTMT eStmt, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcJUMP jump, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcLABEL label, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcMEM mem, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcMOVE move, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcNAME name, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcSEXPR sExpr, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcSTMTS stmts, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcTEMP temp, Arg visArg) {
		throw new Report.InternalError();
	}
	@Override
	public default Result visit(ImcUNOP unOp, Arg visArg) {
		throw new Report.InternalError();
	}

}