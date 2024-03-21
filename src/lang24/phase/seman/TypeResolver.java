package lang24.phase.seman;

import java.util.*;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.*;
import lang24.data.type.*;

/**
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class TypeResolver implements AstFullVisitor<SemType, SemType/*** TODO OR NOT TODO ***/> {

    /**
     * Structural equivalence of types.
     *
     * @param type1 The first type.
     * @param type2 The second type.
     * @return {@code true} if the types are structurally equivalent, {@code false}
     * otherwise.
     */
    private boolean equiv(SemType type1, SemType type2) {
        return equiv(type1, type2, new HashMap<>());
    }

    /**
     * Structural equivalence of types.
     *
     * @param type1  The first type.
     * @param type2  The second type.
     * @param equivs Type synonyms assumed structurally equivalent.
     * @return {@code true} if the types are structurally equivalent, {@code false}
     * otherwise.
     */
    private boolean equiv(SemType type1, SemType type2, Map<SemType, Set<SemType>> equivs) {

        if ((type1 instanceof SemNameType) && (type2 instanceof SemNameType)) {
            if (equivs == null) {
                equivs = new HashMap<>();
            }

            equivs.computeIfAbsent(type1, k -> new HashSet<SemType>());
            equivs.computeIfAbsent(type2, k -> new HashSet<SemType>());

            if (equivs.get(type1).contains(type2) && equivs.get(type2).contains(type1)) {
                return true;
            } else {
                Set<SemType> types;

                types = equivs.get(type1);
                types.add(type2);
                equivs.put(type1, types);

                types = equivs.get(type2);
                types.add(type1);
                equivs.put(type2, types);
            }
        }

        type1 = type1.actualType();
        type2 = type2.actualType();

        if (type1 instanceof SemVoidType) {
            return (type2 instanceof SemVoidType);
        }
        if (type1 instanceof SemBoolType) {
            return (type2 instanceof SemBoolType);
        }
        if (type1 instanceof SemCharType) {
            return (type2 instanceof SemCharType);
        }
        if (type1 instanceof SemIntType) {
            return (type2 instanceof SemIntType);
        }

        if (type1 instanceof SemArrayType arr1) {
            if (!(type2 instanceof SemArrayType arr2)) {
                return false;
            }
            if (arr1.size != arr2.size) {
                return false;
            }

            return equiv(arr1.elemType, arr2.elemType, equivs);
        }

        if (type1 instanceof SemPointerType ptr1) {
            if (!(type2 instanceof SemPointerType ptr2)) {
                return false;
            }
            if ((ptr1.baseType.actualType() instanceof SemVoidType)
                    || (ptr2.baseType.actualType() instanceof SemVoidType)) {
                return true;
            }
            return equiv(ptr1.baseType, ptr2.baseType, equivs);
        }

        if (type1 instanceof SemStructType str1) {
            if (!(type2 instanceof SemStructType str2)) {
                return false;
            }
            if (str1.cmpTypes.size() != str2.cmpTypes.size()) {
                return false;
            }
            for (int c = 0; c < str1.cmpTypes.size(); c++) {
                if (!(equiv(str1.cmpTypes.get(c), str2.cmpTypes.get(c), equivs))) {
                    return false;
                }
            }
            return true;
        }
        if (type1 instanceof SemUnionType uni1) {
            if (!(type2 instanceof SemUnionType uni2)) {
                return false;
            }
            if (uni1.cmpTypes.size() != uni2.cmpTypes.size()) {
                return false;
            }
            for (int c = 0; c < uni1.cmpTypes.size(); c++) {
                if (!(equiv(uni1.cmpTypes.get(c), uni2.cmpTypes.get(c), equivs))) {
                    return false;
                }
            }
            return true;
        }

        throw new Report.InternalError();
    }

    private SemType checkOrThrow(AstNode node, SemType expectedType) {
        return checkOrThrow(node, Set.of(expectedType));
    }


    private SemType checkOrThrow(AstNode node, Set<SemType> expectedTypes) {
        final var actualType = node.accept(this, null);

        final var equivs = new HashMap<SemType, Set<SemType>>();
        SemType expectedType = null;
        for (var typ : expectedTypes) {
            expectedType = typ;
            equivs.put(expectedType, expectedTypes);
        }

        if (!equiv(actualType, expectedType, equivs)) {
            String expectedTypeStr;
            if (expectedTypes.size() == 1) {
                expectedTypeStr = String.format("`%s`", expectedTypes.iterator().next().toString());
            } else {
                expectedTypeStr = String.format("one of [%s]", expectedTypes.stream().map(SemType::toString).reduce((a, b) -> a + ", " + b).get());
            }
            var err = new ErrorAtBuilder("Type mismatch! Expected " + expectedTypeStr + ", got: " + actualType, node.location());
            throw new Report.Error(node, err.toString());
        }

        return actualType;
    }


    // Todo!
    @Override
    public SemType visit(AstTypDefn typDefn, SemType arg) {
        return AstFullVisitor.super.visit(typDefn, arg);
    }


    // Todo!
    @Override
    public SemType visit(AstArrExpr arrExpr, SemType arg) {
        var type = arrExpr.arr.accept(this, arg);
        checkOrThrow(arrExpr.idx, SemIntType.type);

        return type;
    }

    @Override
    public SemType visit(AstAtomExpr atomExpr, SemType arg) {
        return switch (atomExpr.type) {
            case VOID -> SemVoidType.type;
            case BOOL -> SemBoolType.type;
            case CHAR -> SemCharType.type;
            case INT -> SemIntType.type;
            case STR -> SemPointerType.stringType;
            case PTR -> SemPointerType.type;
        };
    }

    @Override
    public SemType visit(AstBinExpr binExpr, SemType arg) {
        Set<SemType> expectedTypes = switch (binExpr.oper) {
            case ADD, SUB, MUL, DIV, MOD -> Set.of(SemIntType.type);
            case EQU, NEQ, LTH, GTH, LEQ, GEQ -> Set.of(SemIntType.type, SemCharType.type, SemBoolType.type, SemPointerType.type);
            case AND, OR -> Set.of(SemBoolType.type);
        };

        var firstType = checkOrThrow(binExpr.fstExpr, expectedTypes);

        return checkOrThrow(binExpr.sndExpr, firstType);
    }

    @Override
    public SemType visit(AstCallExpr callExpr, SemType arg) {
        return AstFullVisitor.super.visit(callExpr, arg);
    }

    @Override
    public SemType visit(AstCastExpr castExpr, SemType arg) {
        return AstFullVisitor.super.visit(castExpr, arg);
    }

    @Override
    public SemType visit(AstCmpExpr cmpExpr, SemType arg) {
        return AstFullVisitor.super.visit(cmpExpr, arg);
    }

    @Override
    public SemType visit(AstNameExpr nameExpr, SemType arg) {
        return AstFullVisitor.super.visit(nameExpr, arg);
    }

    @Override
    public SemType visit(AstPfxExpr pfxExpr, SemType arg) {
        var expectedType = switch (pfxExpr.oper) {
            case ADD, SUB -> SemIntType.type;
            case NOT -> SemBoolType.type;
            case PTR -> SemPointerType.type;
        };

        checkOrThrow(pfxExpr.expr, expectedType);

        return expectedType;
    }

    @Override
    public SemType visit(AstSfxExpr sfxExpr, SemType arg) {
        return AstFullVisitor.super.visit(sfxExpr, arg);
    }

    @Override
    public SemType visit(AstSizeofExpr sizeofExpr, SemType arg) {
        AstFullVisitor.super.visit(sizeofExpr, arg);

        return SemIntType.type;
    }

    @Override
    public SemType visit(AstExprStmt exprStmt, SemType arg) {
        return exprStmt.expr.accept(this, arg);
    }

    @Override
    public SemType visit(AstAssignStmt assignStmt, SemType arg) {
        var typeDst = assignStmt.dst.accept(this, arg);

        checkOrThrow(assignStmt.src, typeDst);

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstBlockStmt blockStmt, SemType arg) {
        AstFullVisitor.super.visit(blockStmt, arg);

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstIfStmt ifStmt, SemType arg) {
        checkOrThrow(ifStmt.cond, SemBoolType.type);

        // Check recursively
        ifStmt.thenStmt.accept(this, arg);
        if (ifStmt.elseStmt != null) {
            ifStmt.elseStmt.accept(this, arg);
        }

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstWhileStmt whileStmt, SemType arg) {
        checkOrThrow(whileStmt.cond, SemBoolType.type);
        whileStmt.stmt.accept(this, arg);

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstReturnStmt retStmt, SemType arg) {
        AstFullVisitor.super.visit(retStmt, arg);
        return SemVoidType.type;
    }


    @Override
    public SemType visit(AstNodes<? extends AstNode> nodes, SemType arg) {
        AstFullVisitor.super.visit(nodes, arg);

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstVarDefn varDefn, SemType arg) {

        return varDefn.type.accept(this, arg);
    }

    @Override
    public SemType visit(AstFunDefn funDefn, SemType arg) {
        if (funDefn.pars != null) {
            funDefn.pars.accept(this, arg);
        }
        if (funDefn.defns != null) {
            funDefn.defns.accept(this, arg);
        }

        var fnType = funDefn.type.accept(this, arg);

        return checkOrThrow(funDefn.stmt, fnType);
    }

    @Override
    public SemType visit(AstFunDefn.AstRefParDefn refParDefn, SemType arg) {
        return refParDefn.type.accept(this, arg);
    }

    @Override
    public SemType visit(AstFunDefn.AstValParDefn valParDefn, SemType arg) {
        return valParDefn.type.accept(this, arg);
    }

    // todo all types
    @Override
    public SemType visit(AstArrType arrType, SemType arg) {
        var elemType = arrType.elemType.accept(this, arg);

        checkOrThrow(arrType.size, SemIntType.type);

        // Try to get size
        if (arrType.size instanceof AstAtomExpr atomExpr) {
            if (atomExpr.type == AstAtomExpr.Type.INT) {
                return new SemArrayType(elemType, Long.parseLong(atomExpr.value));
            }
        }

        // Todo - is this ok?
        return new SemPointerType(elemType);
    }

    @Override
    public SemType visit(AstAtomType atomType, SemType arg) {
        return switch (atomType.type) {
            case VOID -> SemVoidType.type;
            case BOOL -> SemBoolType.type;
            case CHAR -> SemCharType.type;
            case INT -> SemIntType.type;
        };
    }

    @Override
    public SemType visit(AstNameType nameType, SemType arg) {
        // Todo - how to define the type of a named type?
        return new SemNameType(nameType.name);
    }

    @Override
    public SemType visit(AstPtrType ptrType, SemType arg) {
        return new SemPointerType(ptrType.baseType.accept(this, arg));
    }

    @Override
    public SemType visit(AstStrType strType, SemType arg) {
        for (int i = 0; i < strType.cmps.nodes.length; ++i) {
            AstNode cmp = strType.cmps.nodes[i];
            cmp.accept(this, arg);
        }
        return new SemStructType(Collections.emptyList());
    }

    @Override
    public SemType visit(AstUniType uniType, SemType arg) {
        for (int i = 0; i < uniType.cmps.nodes.length; ++i) {
            AstNode cmp = uniType.cmps.nodes[i];
            cmp.accept(this, arg);
        }
        return new SemStructType(Collections.emptyList());
    }

    // Todo
    @Override
    public SemType visit(AstRecType.AstCmpDefn cmpDefn, SemType arg) {
        return cmpDefn.type.accept(this, arg);
    }
}