package lang24.phase.seman;

import lang24.common.report.ErrorAtBuilder;
import lang24.common.report.Report;
import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.AstAssignStmt;
import lang24.data.ast.visitor.*;
import lang24.data.type.SemPointerType;
import lang24.data.type.SemType;
import lang24.data.type.SemVoidType;

import java.util.Optional;

/**
 * Lvalue resolver.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class LValResolver implements AstFullVisitor<Boolean, Object> {

	/** Constructs a new lvalue resolver. */
	public LValResolver() {
	}

	private void throwNotLValue(AstNode expr) {
		var err = new ErrorAtBuilder("The following expression is not a valid lvalue:", expr);
		throw new Report.Error(expr, err.toString());
	}

	@Override
	public Boolean visit(AstVarDefn varDefn, Object arg) {
		SemAn.isLVal.put(varDefn, true);
		return true;
	}

	@Override
	public Boolean visit(AstFunDefn.AstRefParDefn refParDefn, Object arg) {
		SemAn.isLVal.put(refParDefn, true);
		return true;
	}

	@Override
	public Boolean visit(AstFunDefn.AstValParDefn valParDefn, Object arg) {
		SemAn.isLVal.put(valParDefn, true);
		return true;
	}

	@Override
	public Boolean visit(AstSfxExpr sfxExpr, Object arg) {
		// **Izredno grdo**!
		var type = sfxExpr.expr.accept(new TypeResolver(), arg);

		if (type instanceof SemPointerType) {
			SemAn.isLVal.put(sfxExpr, true);
			return true;
		}

        throwNotLValue(sfxExpr);
        return false;
    }

	@Override
	public Boolean visit(AstArrExpr arrExpr, Object arg) {
		var arrLVal = arrExpr.arr.accept(this, arg);

		if (arrLVal != null && arrLVal) {
			SemAn.isLVal.put(arrExpr, true);
			arrExpr.idx.accept(this, arg);

			return true;
		}

		throwNotLValue(arrExpr);
		return false;
	}

	@Override
	public Boolean visit(AstCmpExpr cmpExpr, Object arg) {
		var exprLVal = cmpExpr.expr.accept(this, arg);

		if (exprLVal != null && exprLVal) {
			SemAn.isLVal.put(cmpExpr, true);
			return true;
		}

		throwNotLValue(cmpExpr);
		return false;
	}

	@Override
	public Boolean visit(AstCastExpr castExpr, Object arg) {
		var exprLVal = castExpr.expr.accept(this, arg);

		if (exprLVal != null && exprLVal) {
			SemAn.isLVal.put(castExpr, true);
			return true;
		}

		throwNotLValue(castExpr);
		return false;
	}



	@Override
	public Boolean visit(AstAssignStmt assignStmt, Object arg) {
		var exprLVal = assignStmt.dst.accept(this, arg);

		if (exprLVal != null && exprLVal) {
			return assignStmt.src.accept(this, arg);
		}

		throwNotLValue(assignStmt.dst);
		return false;
	}

	@Override
	public Boolean visit(AstPfxExpr pfxExpr, Object arg) {
		if (pfxExpr.oper == AstPfxExpr.Oper.PTR) {
			var exprLVal = pfxExpr.expr.accept(this, arg);

			if (exprLVal != null && exprLVal) {
				return true;
			}

			throwNotLValue(pfxExpr);
			return false;
		}

		return true;
	}


	@Override
	public Boolean visit(AstNameExpr nameExpr, Object arg) {
		return true;
	}
}
