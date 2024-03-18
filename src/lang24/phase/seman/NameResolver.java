package lang24.phase.seman;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.AstExprStmt;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.*;

/**
 * Name resolver.
 * <p>
 * The name resolver connects each node of an abstract syntax tree where a name
 * is used with the node where it is defined. The only exceptions are struct and
 * union component names which are connected with their definitions by type
 * resolver. The results of the name resolver are stored in
 * {@link lang24.phase.seman.SemAn#definedAt}.
 */
public class NameResolver implements AstFullVisitor<Object, Object> {

	/** Constructs a new name resolver. */
	public NameResolver() {
	}

	/** The symbol table. */
	private final SymbTable symbTable = new SymbTable();

	@Override
	public Object visit(AstTypDefn typDefn, Object arg) {
		var name = typDefn.name;

        try {
            this.symbTable.ins(name, typDefn);
			AstFullVisitor.super.visit(typDefn, arg);
        } catch (SymbTable.CannotInsNameException e) {
			throw new Report.Error(typDefn, "Name " + name + " already defined. Second definition at " + typDefn.location());
		}

        return AstFullVisitor.super.visit(typDefn, arg);
	}

	@Override
	public Object visit(AstVarDefn varDefn, Object arg) {
		var name = varDefn.name;

		try {
			this.symbTable.ins(name, varDefn);
			AstFullVisitor.super.visit(varDefn, arg);
		} catch (SymbTable.CannotInsNameException e) {
			throw new Report.Error(varDefn, "Name " + name + " already defined. Second definition at " + varDefn.location());
		}

		return AstFullVisitor.super.visit(varDefn, arg);
	}

	@Override
	public Object visit(AstFunDefn funDefn, Object arg) {
		var name = funDefn.name;
		try {
			this.symbTable.ins(name, funDefn);

			this.symbTable.newScope();

			if (funDefn.pars != null) {
                funDefn.pars.accept(this, arg);
            }

			if (funDefn.defns != null) {
                funDefn.defns.accept(this, arg);
            }

			if (funDefn.stmt != null) {
                funDefn.stmt.accept(this, arg);
            }

			this.symbTable.oldScope();
		} catch (SymbTable.CannotInsNameException e) {
			throw new Report.Error(funDefn, "Name " + name + " already defined. Second definition at " + funDefn.location());
		}

		return null;
	}


	@Override
	public Object visit(AstFunDefn.AstRefParDefn refParDefn, Object arg) {
		var name = refParDefn.name;

		try {
			this.symbTable.ins(name, refParDefn);
			AstFullVisitor.super.visit(refParDefn, arg);
		} catch (SymbTable.CannotInsNameException e) {
			throw new Report.Error(refParDefn, "Name " + name + " already defined. Second definition at " + refParDefn.location());
		}

		return AstFullVisitor.super.visit(refParDefn, arg);
	}

	@Override
	public Object visit(AstFunDefn.AstValParDefn valParDefn, Object arg) {
		var name = valParDefn.name;

		try {
			this.symbTable.ins(name, valParDefn);
			AstFullVisitor.super.visit(valParDefn, arg);
		} catch (SymbTable.CannotInsNameException e) {
			throw new Report.Error(valParDefn, "Name " + name + " already defined. Second definition at " + valParDefn.location());
		}

		return AstFullVisitor.super.visit(valParDefn, arg);
	}

	// Not yet
	/*@Override
	public Object visit(AstRecType.AstCmpDefn cmpDefn, Object arg) {
		var name = cmpDefn.name;

		try {
			this.symbTable.ins(name, cmpDefn);
			AstFullVisitor.super.visit(cmpDefn, arg);
		} catch (SymbTable.CannotInsNameException e) {
			throw new Report.Error(cmpDefn, "Name " + name + " already defined. Second definition at " + cmpDefn.location());
		}

		return AstFullVisitor.super.visit(cmpDefn, arg);
	}*/


	@Override
	public Object visit(AstCallExpr callExpr, Object arg) {
		var name = callExpr.name;

		// Find the definition of the function
        try {
            var defn = this.symbTable.fnd(name);

			// Connect the call with the definition
			SemAn.definedAt.put(callExpr, defn);

			AstFullVisitor.super.visit(callExpr, arg);
        } catch (SymbTable.CannotFndNameException e) {
            throw new Report.Error(callExpr, "Name " + name + " not defined. Used at " + callExpr.location());
        }

		return null;
	}

	@Override
	public Object visit(AstNameExpr nameExpr, Object arg) {
		var name = nameExpr.name;

		// Find the definition of the name
		try {
			var defn = this.symbTable.fnd(name);

			// Connect the name with the definition
			SemAn.definedAt.put(nameExpr, defn);

			AstFullVisitor.super.visit(nameExpr, arg);
		} catch (SymbTable.CannotFndNameException e) {
			throw new Report.Error(nameExpr, "Name " + name + " not defined. Used at " + nameExpr.location());
		}

		return null;
	}

	@Override
	public Object visit(AstNameType nameType, Object arg) {
		var name = nameType.name;

		// Find the definition of the name
		try {
			var defn = this.symbTable.fnd(name);

			// Connect the name with the definition
			SemAn.definedAt.put(nameType, defn);

			AstFullVisitor.super.visit(nameType, arg);
		} catch (SymbTable.CannotFndNameException e) {
			throw new Report.Error(nameType, "Name " + name + " not defined. Used at " + nameType.location());
		}

		return null;
	}
}