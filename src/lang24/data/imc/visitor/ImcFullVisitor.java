package lang24.data.imc.visitor;

import lang24.data.imc.code.expr.ImcBINOP;
import lang24.data.imc.code.expr.ImcCALL;
import lang24.data.imc.code.expr.ImcCONST;
import lang24.data.imc.code.expr.ImcExpr;
import lang24.data.imc.code.expr.ImcMEM;
import lang24.data.imc.code.expr.ImcNAME;
import lang24.data.imc.code.expr.ImcSEXPR;
import lang24.data.imc.code.expr.ImcTEMP;
import lang24.data.imc.code.expr.ImcUNOP;
import lang24.data.imc.code.stmt.ImcCJUMP;
import lang24.data.imc.code.stmt.ImcESTMT;
import lang24.data.imc.code.stmt.ImcJUMP;
import lang24.data.imc.code.stmt.ImcLABEL;
import lang24.data.imc.code.stmt.ImcMOVE;
import lang24.data.imc.code.stmt.ImcSTMTS;

/**
 * An abstract visitor of the intermediate code.
 * 
 * @author sliva
 *
 * @param <Result> The result the visitor produces.
 * @param <Arg>    The argument the visitor carries around.
 */
public interface ImcFullVisitor<Result, Arg> extends AbstractImcVisitor<Result, Arg> {
	@Override
	default Result visit(ImcBINOP binOp, Arg visArg) {
		binOp.fstExpr.accept(this, visArg);
		binOp.sndExpr.accept(this, visArg);

		return null;
	}

	@Override
	default Result visit(ImcCALL call, Arg visArg) {
		for (ImcExpr callArg : call.args) {
			callArg.accept(this, null);
		}
		return null;
	}

	@Override
	default Result visit(ImcCJUMP cjump, Arg visArg) {
		cjump.cond.accept(this, null);
		return null;
	}

	@Override
	default Result visit(ImcCONST constant, Arg visArg) {
		return null;
	}

	@Override
	default Result visit(ImcESTMT eStmt, Arg visArg) {
		eStmt.expr.accept(this, null);
		return null;
	}

	@Override
	default Result visit(ImcJUMP jump, Arg visArg) {
		return null;
	}

	@Override
	default Result visit(ImcLABEL label, Arg visArg) {
		return null;
	}

	@Override
	default Result visit(ImcMEM mem, Arg visArg) {
		mem.addr.accept(this, null);
		return null;
	}

	@Override
	default Result visit(ImcMOVE move, Arg visArg) {
		if (move.dst instanceof ImcTEMP _temp) {
			move.src.accept(this, null);
			move.dst.accept(this, null);
		} else if (move.dst instanceof ImcMEM mem) {
			mem.addr.accept(this, null);
			move.src.accept(this, null);
		}
		return null;
	}

	@Override
	default Result visit(ImcNAME name, Arg visArg) {
		return null;
	}

	@Override
	default Result visit(ImcSEXPR sExpr, Arg visArg) {
		sExpr.stmt.accept(this, visArg);
		sExpr.expr.accept(this, visArg);
		return null;
	}

	@Override
	default Result visit(ImcSTMTS stmts, Arg visArg) {
		for (var stmt : stmts.stmts) {
			stmt.accept(this, null);
		}
		return null;
	}

	@Override
	default Result visit(ImcTEMP temp, Arg visArg) {
		return null;
	}

	@Override
	default Result visit(ImcUNOP unOp, Arg visArg) {
		unOp.subExpr.accept(this, null);
		return null;
	}
}