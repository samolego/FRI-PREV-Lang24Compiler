package lang24.phase.seman;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.AstBlockStmt;
import lang24.data.ast.tree.stmt.AstExprStmt;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.*;

import java.util.Arrays;

/**
 * Name resolver.
 * <p>
 * The name resolver connects each node of an abstract syntax tree where a name
 * is used with the node where it is defined. The only exceptions are struct and
 * union component names which are connected with their definitions by type
 * resolver. The results of the name resolver are stored in
 * {@link lang24.phase.seman.SemAn#definedAt}.
 */
public class NameResolver implements AstFullVisitor<Object, PassType> {

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
    public Object visit(AstNodes<? extends AstNode> nodes, PassType arg) {
        AstFullVisitor.super.visit(nodes, PassType.FIRST_PASS);
        AstFullVisitor.super.visit(nodes, PassType.SECOND_PASS);

        return null;
    }

    private void defineOrThrow(AstDefn node, String name) {
        try {
            this.symbTable.ins(name, node);
        } catch (SymbTable.CannotInsNameException e) {
            throw new Report.Error(node, "Name " + name + " already defined. Second definition at " + node.location());
        }

    }


    @Override
    public Object visit(AstBlockStmt blockStmt, PassType arg) {
        this.symbTable.newScope();
        AstFullVisitor.super.visit(blockStmt, arg);
        this.symbTable.oldScope();

        return null;
    }

    @Override
    public Object visit(AstTypDefn typDefn, PassType arg) {
        if (arg == PassType.FIRST_PASS) {
            var name = typDefn.name;
            defineOrThrow(typDefn, name);
        }

        return AstFullVisitor.super.visit(typDefn, arg);
    }

    @Override
    public Object visit(AstVarDefn varDefn, PassType arg) {
        if (arg == PassType.FIRST_PASS) {
            var name = varDefn.name;
            defineOrThrow(varDefn, name);
        }

        return AstFullVisitor.super.visit(varDefn, arg);
    }

    @Override
    public Object visit(AstFunDefn funDefn, PassType arg) {
        switch (arg) {
            case FIRST_PASS -> {
                var name = funDefn.name;
                defineOrThrow(funDefn, name);
            }
            case SECOND_PASS -> {
                this.symbTable.newScope();

                if (funDefn.pars != null) {
                    funDefn.pars.accept(this, null);
                }

                this.symbTable.newScope();

                if (funDefn.defns != null) {
                    funDefn.defns.accept(this, null);
                }

                if (funDefn.stmt != null) {
                    funDefn.stmt.accept(this, null);
                }

                this.symbTable.oldScope();
                this.symbTable.oldScope();
            }
        }

        return null;
    }


    @Override
    public Object visit(AstFunDefn.AstRefParDefn refParDefn, PassType arg) {
        if (arg == PassType.FIRST_PASS) {
            var name = refParDefn.name;
            defineOrThrow(refParDefn, name);
        }

        return AstFullVisitor.super.visit(refParDefn, arg);
    }

    @Override
    public Object visit(AstFunDefn.AstValParDefn valParDefn, PassType arg) {
        if (arg == PassType.FIRST_PASS) {
            var name = valParDefn.name;
            defineOrThrow(valParDefn, name);
        }

        return AstFullVisitor.super.visit(valParDefn, arg);
    }

    // Not yet
	/*@Override
	public Object visit(AstRecType.AstCmpDefn cmpDefn, PassType arg) {
		var name = cmpDefn.name;

		try {
			this.symbTable.ins(name, cmpDefn);
			AstFullVisitor.super.visit(cmpDefn, arg);
		} catch (SymbTable.CannotInsNameException e) {
			throw new Report.Error(cmpDefn, "Name " + name + " already defined. Second definition at " + cmpDefn.location());
		}

		return AstFullVisitor.super.visit(cmpDefn, arg);
	}*/


    private void findOrThrow(AstNode node, String name) {
        try {
            var defn = this.symbTable.fnd(name);
            // Connect the call with the definition
            SemAn.definedAt.put(node, defn);
        } catch (SymbTable.CannotFndNameException e) {
            throw new Report.Error(node, "Name " + name + " not defined. Used at " + node.location());
        }
    }


    @Override
    public Object visit(AstCallExpr callExpr, PassType arg) {
        if (arg == PassType.SECOND_PASS) {
            var name = callExpr.name;
            findOrThrow(callExpr, name);
        }

        return AstFullVisitor.super.visit(callExpr, arg);
    }


    @Override
    public Object visit(AstNameExpr nameExpr, PassType arg) {
        if (arg == PassType.SECOND_PASS) {
            var name = nameExpr.name;
            findOrThrow(nameExpr, name);
        }

        return AstFullVisitor.super.visit(nameExpr, arg);
    }

    @Override
    public Object visit(AstNameType nameType, PassType arg) {
        if (arg == PassType.SECOND_PASS) {
            var name = nameType.name;
            findOrThrow(nameType, name);
        }

        return AstFullVisitor.super.visit(nameType, arg);
    }
}