package lang24.phase.seman;

import lang24.common.report.ErrorAtBuilder;
import lang24.common.report.Report;
import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstTypDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.visitor.AstFullVisitor;

/**
 * Lvalue resolver. This fills the isLVal attribute of expressions.
 * Some checks are performed in TypeResolver.
 */
public class LValResolver implements AstFullVisitor<Boolean, Object> {

	/** Constructs a new lvalue resolver. */
	public LValResolver() {
	}

	public static void throwNotLValue(AstNode expr) {
		var err = new ErrorAtBuilder("The following expression is not a valid lvalue:")
				.addSourceLine(expr)
				.addOffsetedSquiglyLines(expr, "Hint: Try replacing this with valid lvalue.");
		throw new Report.Error(expr, err);
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
		// Handled in TypeResolver
		sfxExpr.expr.accept(this, arg);
		return true;
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
			castExpr.type.accept(this, arg);

			return true;
		}

		return false;
	}

	@Override
	public Boolean visit(AstNameExpr nameExpr, Object arg) {
		var defn = SemAn.definedAt.get(nameExpr);

		if (defn instanceof AstTypDefn) {
			return false;
		}

		SemAn.isLVal.put(nameExpr, true);
		return true;
	}
}
