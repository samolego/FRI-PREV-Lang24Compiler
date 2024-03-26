package lang24.phase.seman;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.AstBlockStmt;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.*;

import java.util.HashSet;
import java.util.Set;

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

    /**
     * Defines a name or throws an error if it is already defined.
     * @param node The node which name points to.
     * @param name The name to define.
     */
    private void defineOrThrow(AstDefn node, String name) {
        try {
            this.symbTable.ins(name, node);
        } catch (SymbTable.CannotInsNameException e) {
            AstNode location = null;
            try {
                location = this.symbTable.fnd(name);
            } catch (SymbTable.CannotFndNameException ignored) {
            }
            var err = new ErrorAtBuilder("Name `" + name + "` is defined here:")
                    .addUnderlinedSourceNode(location)
                    .addLine("\nBut second definition was found here:")
                    .addSourceLine(node)
                    .addOffsetedSquiglyLines(node, "Hint: Try using a different name for this definition.")
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


    // Todo
    @Override
    public Object visit(AstRecType.AstCmpDefn cmpDefn, PassType arg) {
        if (arg == PassType.FIRST_PASS) {
            var name = cmpDefn.name;
            // Define the name in the node
            var parent = (AstRecType) cmpDefn.parent.parent;

            var existingDefn = parent.cmpTypes.get(name);
            // Check if the name is already defined
            if (existingDefn != null) {
                var err = new ErrorAtBuilder("Name `" + name + "` was defined at least twice in the struct:")
                        .addSourceLine(parent)
                        .addOffsetedSquiglyLines(existingDefn, "First definition occurred here.")
                        .addLine("\nBut second definition was found here:")
                        .addSourceLine(parent)
                        .addOffsetedSquiglyLines(cmpDefn, "Hint: Try using a different name for this definition.")
                        .toString();
                throw new Report.Error(cmpDefn, err);
            } else {
                parent.cmpTypes.put(name, cmpDefn);
            }
        }

        return AstFullVisitor.super.visit(cmpDefn, arg);
    }


    @Override
    public Object visit(AstCmpExpr cmpExpr, PassType arg) {
        if (arg == PassType.SECOND_PASS) {
            // todo - what if not name expr?
            if (cmpExpr.expr instanceof AstNameExpr nameExpr) {
                connectOrThrow(cmpExpr, nameExpr.name);
            }
        }
        return AstFullVisitor.super.visit(cmpExpr, arg);
    }

    /**
     * Find the definition of a name or throw an error.
     *
     * @param node The node where the name is used.
     * @param name The name to find.
     */
    private void connectOrThrow(AstNode node, String name) {
        try {
            var defn = this.symbTable.fnd(name);
            // Connect the usage with the definition
            SemAn.definedAt.put(node, defn);

            Set<AstNode> visited = new HashSet<>(Set.of(node, defn));

            var prevDfn = defn;
            while (defn != null) {
                defn = SemAn.definedAt.get(defn.type);
                if (visited.contains(defn)) {
                    var err = new ErrorAtBuilder("Cyclic dependency detected:")
                            .addUnderlinedSourceNode(defn.type)
                            .addSourceLine(prevDfn.type)
                            .addOffsetedSquiglyLines(node, "Hint: Try removing one of the definitions.")
                            .toString();
                    throw new Report.Error(node, err);
                }
                visited.add(defn);
                prevDfn = defn;
            }
        } catch (SymbTable.CannotFndNameException e) {
            var err = new ErrorAtBuilder("Name `" + name + "` is not defined. Used here:")
                    .addSourceLine(node)
                    .addOffsetedSquiglyLines(node, "Hint: Try adding a definition named `" + name + "` before using it.")
                    .toString();
            throw new Report.Error(node, err);
        }
    }


    @Override
    public Object visit(AstCallExpr callExpr, PassType arg) {
        if (arg == PassType.SECOND_PASS) {
            var name = callExpr.name;
            connectOrThrow(callExpr, name);
        }

        return AstFullVisitor.super.visit(callExpr, arg);
    }


    @Override
    public Object visit(AstNameExpr nameExpr, PassType arg) {
        if (arg == PassType.SECOND_PASS) {
            var name = nameExpr.name;
            connectOrThrow(nameExpr, name);
        }

        return AstFullVisitor.super.visit(nameExpr, arg);
    }

    @Override
    public Object visit(AstNameType nameType, PassType arg) {
        if (arg == PassType.SECOND_PASS) {
            var name = nameType.name;
            connectOrThrow(nameType, name);
        }

        return AstFullVisitor.super.visit(nameType, arg);
    }
}