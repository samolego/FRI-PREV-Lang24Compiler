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
    private final LookupTable<AstDefn> symbTable = new LookupTable<>();


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
        } catch (LookupTable.CannotInsNameException e) {
            Location location = null;
            try {
                location = this.symbTable.fnd(name).location();
            } catch (LookupTable.CannotFndNameException ignored) {
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
        this.symbTable.pushScope();
        AstFullVisitor.super.visit(blockStmt, arg);
        this.symbTable.popScope();

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
                funDefn.type.accept(this,  PassType.SECOND_PASS);

                this.symbTable.pushScope();

                if (funDefn.pars != null) {
                    funDefn.pars.accept(this, null);
                }

                this.symbTable.pushScope();

                if (funDefn.defns != null) {
                    funDefn.defns.accept(this, null);
                }

                if (funDefn.stmt != null) {
                    funDefn.stmt.accept(this, PassType.SECOND_PASS);
                }

                this.symbTable.popScope();
                this.symbTable.popScope();
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


    /*@Override
    public Object visit(AstRecType.AstCmpDefn cmpDefn, PassType arg) {
        if (arg == PassType.FIRST_PASS) {
            var name = cmpDefn.name;
            defineOrThrow(cmpDefn, name);
        }

        return AstFullVisitor.super.visit(cmpDefn, arg);
    }*/


    @Override
    public Object visit(AstCmpExpr cmpExpr, PassType arg) {
        // Get type of cmpExpr.expr and check if it has a field with name cmpExpr.name todo
        return AstFullVisitor.super.visit(cmpExpr, arg);
    }

    /**
     * Find the definition of a name or throw an error.
     *
     * @param node The node where the name is used.
     * @param name The name to find.
     */
    private void findOrThrow(AstNode node, String name) {
        try {
            var defn = this.symbTable.fnd(name);
            // Connect the usage with the definition
            SemAn.definedAt.put(node, defn);

            // Todo - check for cyclic dependencies
        } catch (LookupTable.CannotFndNameException e) {
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