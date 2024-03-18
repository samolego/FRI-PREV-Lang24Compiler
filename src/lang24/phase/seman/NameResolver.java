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
public class NameResolver implements AstFullVisitor<Object, Integer> {

    /**
     * Constructs a new name resolver.
     */
    public NameResolver() {
    }

    /**
     * The symbol table.
     */
    private final SymbTable symbTable = new SymbTable();

    @Override
    public Object visit(AstNodes<? extends AstNode> nodes, Integer arg) {
        if (arg == null) {
            // Root of the AST, need to make an additional visit of all the nodes
            AstFullVisitor.super.visit(nodes, 1);
            arg = 2;
        }

        return AstFullVisitor.super.visit(nodes, arg);
    }

    @Override
    public Object visit(AstTypDefn typDefn, Integer arg) {
        if (arg == 1) {
            var name = typDefn.name;

            try {
                this.symbTable.ins(name, typDefn);
            } catch (SymbTable.CannotInsNameException e) {
                throw new Report.Error(typDefn, "Name " + name + " already defined. Second definition at " + typDefn.location());
            }
        }

        return AstFullVisitor.super.visit(typDefn, arg);
    }

    @Override
    public Object visit(AstVarDefn varDefn, Integer arg) {
        if (arg == 1) {
            var name = varDefn.name;

            try {
                this.symbTable.ins(name, varDefn);
            } catch (SymbTable.CannotInsNameException e) {
                throw new Report.Error(varDefn, "Name " + name + " already defined. Second definition at " + varDefn.location());
            }
        }

        return AstFullVisitor.super.visit(varDefn, arg);
    }

    @Override
    public Object visit(AstFunDefn funDefn, Integer arg) {
        if (arg == 1) {
            var name = funDefn.name;
            try {
                this.symbTable.ins(name, funDefn);
            } catch (SymbTable.CannotInsNameException e) {
                throw new Report.Error(funDefn, "Name " + name + " already defined. Second definition at " + funDefn.location());
            }
        }


        if (arg == 1) {
            this.symbTable.newScope();
        }

        if (funDefn.pars != null) {
            funDefn.pars.accept(this, arg);
        }

        if (arg == 1) {
            this.symbTable.newScope();
        }

        if (funDefn.defns != null) {
            funDefn.defns.accept(this, arg);
        }

        if (funDefn.stmt != null) {
            funDefn.stmt.accept(this, arg);
        }

        if (arg == 1) {
            this.symbTable.oldScope();
            this.symbTable.oldScope();
        }

        return null;
    }


    @Override
    public Object visit(AstFunDefn.AstRefParDefn refParDefn, Integer arg) {
        if (arg == 1) {
            var name = refParDefn.name;
            try {
                this.symbTable.ins(name, refParDefn);
            } catch (SymbTable.CannotInsNameException e) {
                throw new Report.Error(refParDefn, "Name " + name + " already defined. Second definition at " + refParDefn.location());
            }
        }

        return AstFullVisitor.super.visit(refParDefn, arg);
    }

    @Override
    public Object visit(AstFunDefn.AstValParDefn valParDefn, Integer arg) {
        var name = valParDefn.name;

        if (arg == 1) {
            try {
                this.symbTable.ins(name, valParDefn);
            } catch (SymbTable.CannotInsNameException e) {
                throw new Report.Error(valParDefn, "Name " + name + " already defined. Second definition at " + valParDefn.location());
            }
        }

        return AstFullVisitor.super.visit(valParDefn, arg);
    }

    // Not yet
	/*@Override
	public Object visit(AstRecType.AstCmpDefn cmpDefn, Integer arg) {
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
    public Object visit(AstCallExpr callExpr, Integer arg) {
        if (arg == 2) {
            var name = callExpr.name;

            // Find the definition of the function
            try {
                var defn = this.symbTable.fnd(name);

                // Connect the call with the definition
                SemAn.definedAt.put(callExpr, defn);
            } catch (SymbTable.CannotFndNameException e) {
                throw new Report.Error(callExpr, "Name " + name + " not defined. Used at " + callExpr.location());
            }
        }

        return AstFullVisitor.super.visit(callExpr, arg);
    }

    @Override
    public Object visit(AstNameExpr nameExpr, Integer arg) {
        if (arg == 2) {
            var name = nameExpr.name;

            // Find the definition of the name
            try {
                var defn = this.symbTable.fnd(name);

                // Connect the name with the definition
                SemAn.definedAt.put(nameExpr, defn);

            } catch (SymbTable.CannotFndNameException e) {
                throw new Report.Error(nameExpr, "Name " + name + " not defined. Used at " + nameExpr.location());
            }
        }

        return AstFullVisitor.super.visit(nameExpr, arg);
    }

    @Override
    public Object visit(AstNameType nameType, Integer arg) {
        // Find the definition of the name
        if (arg == 2) {
            var name = nameType.name;
            try {
                var defn = this.symbTable.fnd(name);

                // Connect the name with the definition
                SemAn.definedAt.put(nameType, defn);

            } catch (SymbTable.CannotFndNameException e) {
                throw new Report.Error(nameType, "Name " + name + " not defined. Used at " + nameType.location());
            }
        }

        return AstFullVisitor.super.visit(nameType, arg);
    }
}