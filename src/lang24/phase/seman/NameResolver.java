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
            Arrays.stream(nodes.nodes).sorted((a, b) -> {
                if (a instanceof AstDefn ad && b instanceof AstDefn bd) {
                    return ad.compareTo(bd);
                }
                return 0;
            }).forEach(node -> node.accept(this, 1));

            arg = 2;
        }

        final Integer finalArg = arg;

        Arrays.stream(nodes.nodes).sorted((a, b) -> {
            if (a instanceof AstDefn ad && b instanceof AstDefn bd) {
                return ad.compareTo(bd);
            }
            return 0;
        }).forEach(node -> node.accept(this, finalArg));

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
    public Object visit(AstBlockStmt blockStmt, Integer arg) {
        this.symbTable.newScope();
        AstFullVisitor.super.visit(blockStmt, arg);
        this.symbTable.oldScope();

        return null;
    }

    @Override
    public Object visit(AstTypDefn typDefn, Integer arg) {
        if (arg == 1) {
            var name = typDefn.name;
            defineOrThrow(typDefn, name);
        }

        return AstFullVisitor.super.visit(typDefn, arg);
    }

    @Override
    public Object visit(AstVarDefn varDefn, Integer arg) {
        if (arg == 1) {
            var name = varDefn.name;
            defineOrThrow(varDefn, name);
        }

        return AstFullVisitor.super.visit(varDefn, arg);
    }

    @Override
    public Object visit(AstFunDefn funDefn, Integer arg) {
        if (arg == 1) {
            var name = funDefn.name;
            defineOrThrow(funDefn, name);
        }

        this.symbTable.newScope();

        if (funDefn.pars != null) {
            funDefn.pars.accept(this, 1);
            funDefn.pars.accept(this, 2);
        }

            this.symbTable.newScope();

        if (funDefn.defns != null) {
            funDefn.defns.accept(this, 1);
            funDefn.defns.accept(this, 2);
        }

        if (funDefn.stmt != null) {
            funDefn.stmt.accept(this, 1);
            funDefn.stmt.accept(this, 2);
        }

            this.symbTable.oldScope();
            this.symbTable.oldScope();

        return null;
    }


    @Override
    public Object visit(AstFunDefn.AstRefParDefn refParDefn, Integer arg) {
        if (arg == 1) {
            var name = refParDefn.name;
            defineOrThrow(refParDefn, name);
        }

        return AstFullVisitor.super.visit(refParDefn, arg);
    }

    @Override
    public Object visit(AstFunDefn.AstValParDefn valParDefn, Integer arg) {
        if (arg == 1) {
            var name = valParDefn.name;
            defineOrThrow(valParDefn, name);
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
    public Object visit(AstCallExpr callExpr, Integer arg) {
        if (arg == 2) {
            var name = callExpr.name;
            findOrThrow(callExpr, name);
        }

        return AstFullVisitor.super.visit(callExpr, arg);
    }


    @Override
    public Object visit(AstNameExpr nameExpr, Integer arg) {
        if (arg == 2) {
            var name = nameExpr.name;
            findOrThrow(nameExpr, name);
        }

        return AstFullVisitor.super.visit(nameExpr, arg);
    }

    @Override
    public Object visit(AstNameType nameType, Integer arg) {
        if (arg == 2) {
            var name = nameType.name;
            findOrThrow(nameType, name);
        }

        return AstFullVisitor.super.visit(nameType, arg);
    }
}