package lang24.phase.seman;

import java.util.*;

import lang24.common.StringUtil;
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
public class TypeResolver implements AstFullVisitor<SemType, TypeResolver.FoundReturnType> {

    private static final Map<SemRecordType, AstRecType> record2ast = new TreeMap<>(Comparator.comparing(semRecordType -> semRecordType.id));


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
     * @param foundType    argument which pass we are in
     * @throws Report.Error if the type of the node is not the expected type.
     */
    private void checkOrThrow(AstNode node, SemType expectedType, FoundReturnType foundType) {
        checkOrThrow(node, Set.of(expectedType), foundType);
    }


    /**
     * Check if the type of the node is one of the expected types.
     *
     * @param node          The node to check.
     * @param expectedTypes Any allowed types that node should be.
     * @param foundType           which pass we are in
     * @return The type of the node (will be one of the expected types).
     * @throws Report.Error if the type of the node is not one of the expected types.
     */
    private SemType checkOrThrow(AstNode node, Set<SemType> expectedTypes, FoundReturnType foundType) {
        final var actualType = node.accept(this, foundType);

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


    // todo - self pointers don't work
    @Override
    public SemType visit(AstTypDefn typDefn, FoundReturnType foundType) {
        var type = typDefn.type.accept(this, foundType);
        SemAn.isType.put(typDefn, type);

        return type;
    }


    @Override
    public SemType visit(AstArrExpr arrExpr, FoundReturnType foundType) {
        var type = arrExpr.arr.accept(this, foundType);
        checkOrThrow(arrExpr.idx, SemIntType.type, foundType);

        var tp = ((SemArrayType) type.actualType()).elemType;

        SemAn.ofType.put(arrExpr, tp);

        return tp;
    }

    @Override
    public SemType visit(AstAtomExpr atomExpr, FoundReturnType foundType) {
        var type = switch (atomExpr.type) {
            case VOID -> SemVoidType.type;
            case BOOL -> SemBoolType.type;
            case CHAR -> SemCharType.type;
            case INT -> SemIntType.type;
            case STR -> SemPointerType.stringType;
            case PTR -> SemPointerType.type;
        };

        SemAn.ofType.put(atomExpr, type);
        return type;
    }

    @Override
    public SemType visit(AstBinExpr binExpr, FoundReturnType foundType) {
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

        var firstType = checkOrThrow(binExpr.fstExpr, expectedTypes, foundType);

        checkOrThrow(binExpr.sndExpr, firstType, foundType);

        SemAn.ofType.put(binExpr, result);
        return result;
    }

    @Override
    public SemType visit(AstCallExpr callExpr, FoundReturnType foundType) {
        if (callExpr.args != null) {
            callExpr.args.accept(this, foundType);
        }

        SemAn.ofType.put(callExpr, SemVoidType.type);
        return findVariableType(callExpr);
    }

    @Override
    public SemType visit(AstCastExpr castExpr, FoundReturnType foundType) {
        var type = castExpr.type.accept(this, foundType);

        checkOrThrow(castExpr.expr, Set.of(SemCharType.type, SemIntType.type, SemPointerType.type), foundType);

        SemAn.ofType.put(castExpr, type);
        return type;
    }

    // todo
    @Override
    public SemType visit(AstCmpExpr cmpExpr, FoundReturnType foundType) {
        var defn = findRecordVariableDefinition(cmpExpr);

        // Check if cmpExpr.expr has a child
        var childDefn = defn.cmpTypes.get(cmpExpr.name);

        // Error - programmer tried to access invalid component
        if (childDefn == null) {
            // Loop through all nodes and try to find similar names
            AstRecType.AstCmpDefn similar = null;
            int min = Integer.MAX_VALUE;
            for (var cmp : defn.cmpTypes.values()) {
                if (similar == null) {
                    similar = cmp;
                    min = StringUtil.calculate(similar.name, cmpExpr.name);
                } else {
                    int dist = StringUtil.calculate(cmp.name, cmpExpr.name);

                    if (dist < min) {
                        similar = cmp;
                        min = dist;
                    }
                }
            }
            var err = new ErrorAtBuilder("Tried to access invalid component `" + cmpExpr.name + "`:")
                    .addSourceLine(cmpExpr);
            if (similar != null && min < 3) {
                err.addOffsetedSquiglyLines(cmpExpr, "Hint: Did you mean `" + similar.name + "`?");
            } else {
                err.addOffsetedSquiglyLines(cmpExpr, "");
            }
            err.addLine("Note: the record is defined with these components:")
                    .addSourceLine(defn);
            throw new Report.Error(err.toString());
        }

        var type = childDefn.accept(this, foundType);

        SemAn.ofType.put(cmpExpr, type);

        return type;
    }

    @Override
    public SemType visit(AstNameExpr nameExpr, FoundReturnType foundType) {
        var type = findVariableType(nameExpr);

        SemAn.ofType.put(nameExpr, type);
        return type;
    }

    @Override
    public SemType visit(AstPfxExpr pfxExpr, FoundReturnType foundType) {
        var expectedType = switch (pfxExpr.oper) {
            case ADD, SUB -> SemIntType.type;
            case NOT -> SemBoolType.type;
            case PTR -> SemPointerType.type;
        };

        checkOrThrow(pfxExpr.expr, expectedType, foundType);
        SemAn.ofType.put(pfxExpr, expectedType);

        return expectedType;
    }

    @Override
    public SemType visit(AstSfxExpr sfxExpr, FoundReturnType foundType) {
        var retType = switch(sfxExpr.oper) {
            case AstSfxExpr.Oper.PTR -> {
                var type = sfxExpr.expr.accept(this, foundType);

                if (type instanceof SemPointerType ptrType) {
                    yield ptrType.baseType;
                }

                var err = new ErrorAtBuilder("Dereference operator `*` can only be applied to a pointer type, but got `" + type + "`:")
                        .addSourceLine(sfxExpr);
                throw new Report.Error(err.toString());
            }
        };

        SemAn.ofType.put(sfxExpr, retType);
        return retType;
    }

    @Override
    public SemType visit(AstSizeofExpr sizeofExpr, FoundReturnType foundType) {
        AstFullVisitor.super.visit(sizeofExpr, foundType);

        SemAn.ofType.put(sizeofExpr, SemIntType.type);
        return SemIntType.type;
    }

    @Override
    public SemType visit(AstExprStmt exprStmt, FoundReturnType foundType) {
        var type = exprStmt.expr.accept(this, foundType);
        SemAn.ofType.put(exprStmt, type);

        return type;
    }

    @Override
    public SemType visit(AstAssignStmt assignStmt, FoundReturnType foundType) {
        var typeDst = assignStmt.dst.accept(this, foundType);

        checkOrThrow(assignStmt.src, typeDst, foundType);

        SemAn.ofType.put(assignStmt, SemVoidType.type);
        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstBlockStmt blockStmt, FoundReturnType foundType) {
        AstFullVisitor.super.visit(blockStmt, foundType);

        SemAn.ofType.put(blockStmt, SemVoidType.type);
        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstIfStmt ifStmt, FoundReturnType foundType) {
        checkOrThrow(ifStmt.cond, SemBoolType.type, foundType);

        // Check recursively
        ifStmt.thenStmt.accept(this, foundType);
        if (ifStmt.elseStmt != null) {
            ifStmt.elseStmt.accept(this, foundType);
        }

        SemAn.ofType.put(ifStmt, SemVoidType.type);

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstWhileStmt whileStmt, FoundReturnType foundType) {
        checkOrThrow(whileStmt.cond, SemBoolType.type, foundType);
        whileStmt.stmt.accept(this, foundType);

        SemAn.ofType.put(whileStmt, SemVoidType.type);
        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstReturnStmt retStmt, FoundReturnType expectedReturnType) {
        if (expectedReturnType != null) {
            // Arg type is expected return type
            expectedReturnType.type = retStmt.expr.accept(this, null);
            expectedReturnType.stmt = retStmt;
        }

        SemAn.ofType.put(retStmt, SemVoidType.type);

        return SemVoidType.type;
    }


    @Override
    public SemType visit(AstNodes<? extends AstNode> nodes, FoundReturnType foundType) {
        SemType type = SemVoidType.type;

        for (final AstNode node : nodes) {
            type = node.accept(this, foundType);
        }

        return type;
    }

    @Override
    public SemType visit(AstVarDefn varDefn, FoundReturnType foundType) {
        var type = varDefn.type.accept(this, foundType);
        SemAn.ofType.put(varDefn, type);

        return type;
    }

    @Override
    public SemType visit(AstFunDefn funDefn, FoundReturnType foundType) {
        var fnType = funDefn.type.accept(this, foundType);

        SemAn.ofType.put(funDefn, fnType);

        // Todo : noben od parametrov ne sme biti void
        // Todo : parametri se štejejo v tip funkcije
        funDefn.pars.accept(this, foundType);
        funDefn.defns.accept(this, foundType);

        var expectedReturnType = new FoundReturnType();
        checkOrThrow(funDefn.stmt, SemVoidType.type, expectedReturnType);
        boolean eq = equiv(fnType, expectedReturnType.type);

        if (!eq) {
            var err = new ErrorAtBuilder("Function `" + funDefn.name() + "` expects to return `" + fnType + "`:")
                    .addSourceLine(funDefn);
            if (expectedReturnType.stmt != null) {
                    err.addOffsetedSquiglyLines(funDefn.type, "`" + fnType + "` is expected here.")
                    .addLine("But the actual return type is `" + expectedReturnType.type + "`:")
                    .addSourceLine(expectedReturnType.stmt)
                    .addOffsetedSquiglyLines(expectedReturnType.stmt.expr, "Hint: Try changing the return type to `" + fnType + "`.");
            } else {
                err.addOffsetedSquiglyLines(funDefn.type, "Note: This function requires returning an expression of type `" + fnType + "`, but no `return` statement was found.")
                    .addSourceLineEnd(funDefn);
            }
            throw new Report.Error(err.toString());
        }

        return fnType;
    }

    // Todo
    @Override
    public SemType visit(AstFunDefn.AstRefParDefn refParDefn, FoundReturnType foundType) {
        var type = refParDefn.type.accept(this, foundType);

        if (type.actualType() == SemVoidType.type) {
            var err = new ErrorAtBuilder("Reference parameter cannot be of type `void`:")
                    .addSourceLine(refParDefn.parent.parent)
                    .addOffsetedSquiglyLines(refParDefn, "");
            throw new Report.Error(err.toString());
        }

        SemAn.ofType.put(refParDefn, type);

        return type;
    }

    @Override
    public SemType visit(AstFunDefn.AstValParDefn valParDefn, FoundReturnType foundType) {
        var type = valParDefn.type.accept(this, foundType);

        if (type.actualType() == SemVoidType.type) {
            var err = new ErrorAtBuilder("Value parameter cannot be of type `void`:")
                    .addSourceLine(valParDefn.parent.parent)
                    .addOffsetedSquiglyLines(valParDefn, "");
            throw new Report.Error(err.toString());
        }

        SemAn.ofType.put(valParDefn, type);

        return type;
    }

    @Override
    public SemType visit(AstArrType arrType, FoundReturnType foundType) {
        var elemType = arrType.elemType.accept(this, foundType);

        checkOrThrow(arrType.size, SemIntType.type, foundType);

        // Try to get size
        if (arrType.size instanceof AstAtomExpr atomExpr) {
            if (atomExpr.type == AstAtomExpr.Type.INT) {
                var type = new SemArrayType(elemType, Long.parseLong(atomExpr.value));

                SemAn.ofType.put(arrType, type);

                return type;
            }
        }

        // Todo - is this ok?
        throw new Report.InternalError();
    }

    @Override
    public SemType visit(AstAtomType atomType, FoundReturnType foundType) {
        var type = switch (atomType.type) {
            case VOID -> SemVoidType.type;
            case BOOL -> SemBoolType.type;
            case CHAR -> SemCharType.type;
            case INT -> SemIntType.type;
        };

        SemAn.isType.put(atomType, type);

        return type;
    }

    @Override
    public SemType visit(AstNameType nameType, FoundReturnType foundType) {
        var type = findTypeAssociation(nameType);

        SemAn.ofType.put(nameType, type);

        return type;
    }

    @Override
    public SemType visit(AstPtrType ptrType, FoundReturnType foundType) {
        var type = new SemPointerType(ptrType.baseType.accept(this, foundType));

        SemAn.ofType.put(ptrType, type);

        return type;
    }

    @Override
    public SemType visit(AstStrType strType, FoundReturnType foundType) {
        var components = new LinkedList<SemType>();

        for (int i = 0; i < strType.cmps.size(); ++i) {
            AstNode cmp = strType.cmps.get(i);
            var type = cmp.accept(this, foundType);

            components.add(type);
        }

        var type = new SemStructType(components);
        record2ast.put(type, strType);

        SemAn.ofType.put(strType, type);

        return type;
    }

    @Override
    public SemType visit(AstUniType uniType, FoundReturnType foundType) {
        var components = new LinkedList<SemType>();

        for (int i = 0; i < uniType.cmps.size(); ++i) {
            AstNode cmp = uniType.cmps.get(i);
            var type = cmp.accept(this, foundType);
            components.add(type);
        }

        var type = new SemUnionType(components);
        record2ast.put(type, uniType);

        SemAn.ofType.put(uniType, type);

        return type;
    }

    @Override
    public SemType visit(AstRecType.AstCmpDefn cmpDefn, FoundReturnType foundType) {
        var type = cmpDefn.type.accept(this, foundType);

        SemAn.ofType.put(cmpDefn, type);
        return type;
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
            type = defined.accept(this, null).actualType();
            SemAn.ofType.put(node, type);
        }

        return type.actualType();
    }


    private AstRecType findRecordVariableDefinition(AstCmpExpr cmpExpr) {
        var type = cmpExpr.expr.accept(this, null);
        var defn = record2ast.get((SemRecordType) type);

        if (defn == null) {
            throw new Report.InternalError();
        }

        return defn;
    }
    
    public static class FoundReturnType {
        private SemType type;
        private AstReturnStmt stmt;

        public FoundReturnType() {
            this.type = SemVoidType.type;
        }
    }
}