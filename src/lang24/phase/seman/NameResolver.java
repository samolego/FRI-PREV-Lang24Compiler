package lang24.phase.seman;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.AstBlockStmt;
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
        if (arg == null) {
            AstFullVisitor.super.visit(nodes, PassType.FIRST_PASS);
            arg = PassType.SECOND_PASS;
        }

        return AstFullVisitor.super.visit(nodes, arg);
    }

    private void defineOrThrow(AstDefn node, String name) {
        try {
            this.symbTable.ins(name, node);
        } catch (SymbTable.CannotInsNameException e) {
            Location location = null;
            try {
                location = this.symbTable.fnd(name).location();
            } catch (SymbTable.CannotFndNameException ignored) {
            }
            var err = new ErrorAtBuilder("Name `" + name + "` is defined here:", location)
                    .addString("But second definition was found here:")
                    .addLocation(node)
                    .toString();
            throw new Report.Error(err);
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
                    funDefn.stmt.accept(this, PassType.SECOND_PASS);
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


    @Override
    public Object visit(AstRecType.AstCmpDefn cmpDefn, PassType arg) {
        if (arg == PassType.FIRST_PASS) {
            var name = cmpDefn.name;
            defineOrThrow(cmpDefn, name);
        }

        return AstFullVisitor.super.visit(cmpDefn, arg);
    }


    @Override
    public Object visit(AstStrType strType, PassType arg) {
        return AstFullVisitor.super.visit(strType, arg);
    }

    @Override
    public Object visit(AstUniType uniType, PassType arg) {
        return AstFullVisitor.super.visit(uniType, arg);
    }

    @Override
    public Object visit(AstCmpExpr cmpExpr, PassType arg) {
        // Validate that the expression cmpExpr.expr is defined and it has member cmpExpr.name
        return AstFullVisitor.super.visit(cmpExpr, arg);
    }

    private void findOrThrow(AstNode node, String name) {
        try {
            var defn = this.symbTable.fnd(name);
            // Connect the call with the definition
            SemAn.definedAt.put(node, defn);
        } catch (SymbTable.CannotFndNameException e) {
            var err = new ErrorAtBuilder("Name `" + name + "` not defined. Used here: ", node.location())
                    .toString();
            throw new Report.Error(err);
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