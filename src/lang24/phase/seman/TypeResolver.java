package lang24.phase.seman;

import java.util.*;

import lang24.common.report.*;
import lang24.data.ast.tree.Nameable;
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
public class TypeResolver implements AstFullVisitor<SemType, Object> {


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

    /**
     * Check if the type of the node is the expected type.
     *
     * @param node         The node to check.
     * @param expectedType The expected type that node should be.
     * @param arg          which pass we are in
     * @throws Report.Error if the type of the node is not the expected type.
     */
    private void checkOrThrow(AstNode node, SemType expectedType, Object arg) {
        checkOrThrow(node, Set.of(expectedType), arg);
    }


    /**
     * Check if the type of the node is one of the expected types.
     *
     * @param node          The node to check.
     * @param expectedTypes Any allowed types that node should be.
     * @param arg           which pass we are in
     * @return The type of the node (will be one of the expected types).
     * @throws Report.Error if the type of the node is not one of the expected types.
     */
    private SemType checkOrThrow(AstNode node, Set<SemType> expectedTypes, Object arg) {
        final var actualType = node.accept(this, arg);

        boolean eq = false;

        for (SemType expectedType : expectedTypes) {
            if (equiv(actualType, expectedType)) {
                eq = true;
                break;
            }
        }

        if (!eq) {
            String expectedTypeStr;
            if (expectedTypes.size() == 1) {
                expectedTypeStr = String.format("`%s`", expectedTypes.iterator().next().toString());
            } else {
                //noinspection OptionalGetWithoutIsPresent
                expectedTypeStr = String.format("one of [%s]", expectedTypes.stream().map(SemType::toString).reduce((a, b) -> a + ", " + b).get());
            }
            var err = new ErrorAtBuilder("Type mismatch! Expected " + expectedTypeStr + ", but got `" + actualType + "`:")
                    .addSourceLine(node)
                    .addOffsetedSquiglyLines(node, "This expression has type `" + actualType + "`, which is wrong.");
            throw new Report.Error(node, err.toString());
        }

        return actualType;
    }


    @Override
    public SemType visit(AstTypDefn typDefn, Object arg) {
        var type = typDefn.type.accept(this, null);
        SemAn.isType.put(typDefn, type);

        return type;

    }


    @Override
    public SemType visit(AstArrExpr arrExpr, Object arg) {
        var type = arrExpr.arr.accept(this, arg);
        checkOrThrow(arrExpr.idx, SemIntType.type, arg);

        return ((SemArrayType) type.actualType()).elemType;
    }

    @Override
    public SemType visit(AstAtomExpr atomExpr, Object arg) {
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
    public SemType visit(AstBinExpr binExpr, Object arg) {
        SemType result;
        Set<SemType> expectedTypes = switch (binExpr.oper) {
            case ADD, SUB, MUL, DIV, MOD -> {
                result = SemIntType.type;
                yield Set.of(SemIntType.type);
            }
            case EQU, NEQ -> {
                result = SemBoolType.type;
                yield Set.of(SemIntType.type, SemCharType.type, SemBoolType.type, SemPointerType.type);
            }
            case LTH, GTH, LEQ, GEQ -> {
                result = SemBoolType.type;
                yield Set.of(SemIntType.type, SemCharType.type, SemPointerType.type);
            }
            case AND, OR -> {
                result = SemBoolType.type;
                yield Set.of(SemBoolType.type);
            }
        };

        var firstType = checkOrThrow(binExpr.fstExpr, expectedTypes, arg);

        checkOrThrow(binExpr.sndExpr, firstType, arg);

        return result;
    }

    @Override
    public SemType visit(AstCallExpr callExpr, Object arg) {
        if (callExpr.args != null) {
            callExpr.args.accept(this, arg);
        }

        return findVariableType(callExpr);
    }

    @Override
    public SemType visit(AstCastExpr castExpr, Object arg) {
        var type = castExpr.type.accept(this, arg);

        checkOrThrow(castExpr.expr, Set.of(SemCharType.type, SemIntType.type, SemPointerType.type), arg);

        return type;
    }

    // todo
    @Override
    public SemType visit(AstCmpExpr cmpExpr, Object arg) {
        return cmpExpr.expr.accept(this, arg);

    }

    @Override
    public SemType visit(AstNameExpr nameExpr, Object arg) {
        return findVariableType(nameExpr);
    }

    @Override
    public SemType visit(AstPfxExpr pfxExpr, Object arg) {
        var expectedType = switch (pfxExpr.oper) {
            case ADD, SUB -> SemIntType.type;
            case NOT -> SemBoolType.type;
            case PTR -> SemPointerType.type;
        };

        checkOrThrow(pfxExpr.expr, expectedType, arg);

        return expectedType;
    }

    @Override
    public SemType visit(AstSfxExpr sfxExpr, Object arg) {
        return sfxExpr.expr.accept(this, arg);
    }

    @Override
    public SemType visit(AstSizeofExpr sizeofExpr, Object arg) {
        AstFullVisitor.super.visit(sizeofExpr, arg);

        return SemIntType.type;
    }

    @Override
    public SemType visit(AstExprStmt exprStmt, Object arg) {
        return exprStmt.expr.accept(this, arg);
    }

    @Override
    public SemType visit(AstAssignStmt assignStmt, Object arg) {
        var typeDst = assignStmt.dst.accept(this, arg);

        checkOrThrow(assignStmt.src, typeDst, arg);

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstBlockStmt blockStmt, Object arg) {
        AstFullVisitor.super.visit(blockStmt, arg);

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstIfStmt ifStmt, Object arg) {
        checkOrThrow(ifStmt.cond, SemBoolType.type, arg);

        // Check recursively
        ifStmt.thenStmt.accept(this, arg);
        if (ifStmt.elseStmt != null) {
            ifStmt.elseStmt.accept(this, arg);
        }

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstWhileStmt whileStmt, Object arg) {
        checkOrThrow(whileStmt.cond, SemBoolType.type, arg);
        whileStmt.stmt.accept(this, arg);

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstReturnStmt retStmt, Object arg) {
        AstFullVisitor.super.visit(retStmt, arg);
        return SemVoidType.type;
    }


    @Override
    public SemType visit(AstNodes<? extends AstNode> nodes, Object arg) {
        SemType type = SemVoidType.type;

        for (final AstNode node : nodes) {
            type = node.accept(this, arg);
        }

        return type;
    }

    @Override
    public SemType visit(AstVarDefn varDefn, Object arg) {
        var type = varDefn.type.accept(this, null);
        SemAn.ofType.put(varDefn, type);

        return type;
    }

    @Override
    public SemType visit(AstFunDefn funDefn, Object arg) {
        var fnType = funDefn.type.accept(this, null);

        SemAn.ofType.put(funDefn, fnType);

        // Todo : noben od parametrov ne sme biti void
        // Todo : parametri se štejejo v tip funkcije
        if (funDefn.pars != null) {
            funDefn.pars.accept(this, null);
        }


        if (funDefn.defns != null) {
            funDefn.defns.accept(this, null);
        }

        checkOrThrow(funDefn.stmt, fnType, null);
        //todo

        return fnType;
    }


    // Todo
    @Override
    public SemType visit(AstFunDefn.AstRefParDefn refParDefn, Object arg) {
        return checkOrThrow(refParDefn.type, Set.of(SemCharType.type, SemIntType.type, SemBoolType.type, SemPointerType.type), arg);
    }

    @Override
    public SemType visit(AstFunDefn.AstValParDefn valParDefn, Object arg) {
        return checkOrThrow(valParDefn.type, Set.of(SemCharType.type, SemIntType.type, SemBoolType.type, SemPointerType.type), arg);
    }

    @Override
    public SemType visit(AstArrType arrType, Object arg) {
        var elemType = arrType.elemType.accept(this, arg);

        checkOrThrow(arrType.size, SemIntType.type, arg);

        // Try to get size
        if (arrType.size instanceof AstAtomExpr atomExpr) {
            if (atomExpr.type == AstAtomExpr.Type.INT) {
                return new SemArrayType(elemType, Long.parseLong(atomExpr.value));
            }
        }

        // Todo - is this ok?
        throw new Report.InternalError();
    }

    @Override
    public SemType visit(AstAtomType atomType, Object arg) {
        return switch (atomType.type) {
            case VOID -> SemVoidType.type;
            case BOOL -> SemBoolType.type;
            case CHAR -> SemCharType.type;
            case INT -> SemIntType.type;
        };
    }

    @Override
    public SemType visit(AstNameType nameType, Object arg) {
        return findTypeAssociation(nameType);
    }

    @Override
    public SemType visit(AstPtrType ptrType, Object arg) {
        return new SemPointerType(ptrType.baseType.accept(this, arg));
    }

    @Override
    public SemType visit(AstStrType strType, Object arg) {
        var components = new LinkedList<SemType>();

        for (int i = 0; i < strType.cmps.size(); ++i) {
            AstNode cmp = strType.cmps.get(i);
            var type = cmp.accept(this, arg);

            components.add(type);
        }

        return new SemStructType(components);
    }

    @Override
    public SemType visit(AstUniType uniType, Object arg) {
        var components = new LinkedList<SemType>();

        for (int i = 0; i < uniType.cmps.size(); ++i) {
            AstNode cmp = uniType.cmps.get(i);
            var type = cmp.accept(this, arg);
            components.add(type);
        }

        return new SemStructType(components);
    }

    // Todo
    @Override
    public SemType visit(AstRecType.AstCmpDefn cmpDefn, Object arg) {
        return cmpDefn.type.accept(this, arg);
    }


    private SemType findTypeAssociation(AstNameType node) {
        var defined = SemAn.definedAt.get(node);
        var type = SemAn.isType.get(defined);

        if (type == null) {
            type = SemAn.ofType.get(defined);

            if (type != null) {
                var err = new ErrorAtBuilder("Name `" + node.name() + "` is actually a variable, but was used as type here:", node);
                throw new Report.Error(err.toString());
            }

            // Ok, not, let's define it
            type = defined.accept(this, null);
            SemAn.isType.put(defined, type);
        }

        return type.actualType();
    }


    private <T extends AstNode & Nameable> SemType findVariableType(T node) {
        var defined = SemAn.definedAt.get(node);
        var type = SemAn.ofType.get(defined);

        if (type == null) {
            // It was not found, perhaps programmer tried to access type as variable?
            type = SemAn.isType.get(defined);

            if (type != null) {
                var err = new ErrorAtBuilder("Name `" + node.name() + "` is actually a type, but was used as variable here:", node);
                throw new Report.Error(err.toString());
            }

            // Ok, not, let's define it
            type = defined.accept(this, null);
            SemAn.ofType.put(defined, type);
        }

        return type.actualType();
    }
}