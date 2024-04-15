package lang24.phase.seman;

import lang24.common.StringUtil;
import lang24.common.report.ErrorAtBuilder;
import lang24.common.report.Report;
import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.tree.Nameable;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstTypDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.type.*;

import java.util.*;

/**
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class TypeResolver implements AstFullVisitor<SemType, Object> {

    private static final Map<SemRecordType, AstRecType> record2ast = new TreeMap<>(Comparator.comparing(semRecordType -> semRecordType.id));

    /**
     * Return type that was found when parsing function.
     */
    private FoundReturnType foundReturnType = null;
    private AstFunDefn currentReturningFunction = null;


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
     * @param arg          argument which pass we are in
     * @throws Report.Error if the type of the node is not the expected type.
     */
    private SemType checkOrThrow(AstNode node, SemType expectedType, Object arg) {
        return checkOrThrow(node, Set.of(expectedType), arg);
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
            throw new Report.Error(node, err);
        }

        return actualType;
    }


    /**
     * Check if the node is a lvalue or throw an error.
     *
     * @param node The node to check.
     */
    private void checkLValueOrThrow(AstNode node) {
        Boolean islval = SemAn.isLVal.get(node);

        if (islval == null || !islval) {
            LValResolver.throwNotLValue(node);
        }
    }


    @Override
    public SemType visit(AstTypDefn typDefn, Object arg) {
        if (SemAn.isType.get(typDefn) != null) {
            return SemAn.isType.get(typDefn);
        }

        // Create dummy nametype to proccess self-referencing types
        var nameType = new SemNameType(typDefn.name());
        nameType.define(SemVoidType.type);
        SemAn.isType.put(typDefn, nameType);
        // End of dummy

        // Actual type
        var type = typDefn.type.accept(this, arg);

        nameType = new SemNameType(typDefn.name());
        nameType.define(type);
        SemAn.isType.put(typDefn, nameType);

        return nameType;
    }


    @Override
    public SemType visit(AstArrExpr arrExpr, Object arg) {

        checkOrThrow(arrExpr.idx, SemIntType.type, arg);

        var type = arrExpr.arr.accept(this, arg);
        var tp = ((SemArrayType) type.actualType()).elemType;

        SemAn.ofType.put(arrExpr, tp);

        checkLValueOrThrow(arrExpr.arr);
        return tp;
    }

    @Override
    public SemType visit(AstAtomExpr atomExpr, Object arg) {
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

        SemAn.ofType.put(binExpr, result);
        return result;
    }

    @Override
    public SemType visit(AstCallExpr callExpr, Object arg) {
        AstFunDefn funDefn = (AstFunDefn) SemAn.definedAt.get(callExpr);
        var callPars = callExpr.args;

        // Check parameters
        int i = 0;
        for (var expParam : funDefn.pars) {
            if (i < callPars.size()) {
                var callParamType = callPars.get(i).accept(this, null);
                var expParamType = expParam.accept(this, null);

                if (!equiv(callParamType, expParamType)) {
                    var err = new ErrorAtBuilder("Type mismatch in function call `" + callExpr.name + "`. Expected `" + expParamType + "`, but got `" + callParamType + "`:")
                            .addSourceLine(callPars.get(i))
                            .addOffsetedSquiglyLines(callPars.get(i), "This expression has type `" + callParamType + "`.")
                            .addLine("")
                            .addLine("But function `" + funDefn.name() + "` expects `" + expParamType + "`:")
                            .addUnderlinedSourceNode(expParam);
                    throw new Report.Error(callExpr, err);
                }
            } else {
                // Too few parameters
                var err = new ErrorAtBuilder("Too few parameters in function call `" + callExpr.name + "`:")
                        .addSourceLine(callExpr)
                        .addOffsetedSquiglyLines(callExpr, "Hint: try adding " + (funDefn.pars.size() - i) + " more parameter(s).")
                        .addLine("")
                        .addLine("Expected `" + expParam.accept(this, null) + "`:")
                        .addUnderlinedSourceNode(expParam);
                throw new Report.Error(callExpr, err);
            }
            i += 1;
        }
        if (i != callPars.size()) {
            // Too many parameters
            var err = new ErrorAtBuilder("Too many parameters in function call `" + callExpr.name + "`:")
                    .addSourceLine(callExpr)
                    .addOffsetedSquiglyLines(callExpr, "Hint: try removing last " + (callPars.size() - i) + " parameter(s).")
                    .addLine("")
                    .addLine("Function `" + funDefn.name() + "` accepts " + funDefn.pars.size() + " parameters:")
                    .addSourceLine(funDefn);
            throw new Report.Error(callExpr, err);
        }

        var type = findVariableType(callExpr);
        SemAn.ofType.put(callExpr, type);

        return type;
    }

    @Override
    public SemType visit(AstCastExpr castExpr, Object arg) {
        var type = castExpr.type.accept(this, arg);

        checkOrThrow(castExpr.expr, Set.of(SemCharType.type, SemIntType.type, SemPointerType.type), arg);

        SemAn.ofType.put(castExpr, type);
        return type;
    }

    @Override
    public SemType visit(AstCmpExpr cmpExpr, Object arg) {
        var defn = findRecordVariableDefinition(cmpExpr);

        // Check if cmpExpr.expr has a child
        var childDefn = defn.cmpTypes.get(cmpExpr.name);

        // Error - programmer tried to access invalid component
        if (childDefn == null) {
            // Loop through all nodes and try to find similar names
            AstRecType.AstCmpDefn similar = null;
            int minDist = Integer.MAX_VALUE;
            for (var cmp : defn.cmpTypes.values()) {
                if (similar == null) {
                    similar = cmp;
                    minDist = StringUtil.calculate(similar.name, cmpExpr.name);
                } else {
                    int dist = StringUtil.calculate(cmp.name, cmpExpr.name);

                    if (dist < minDist) {
                        similar = cmp;
                        minDist = dist;
                    }
                }
            }
            var err = new ErrorAtBuilder("Tried to access invalid component `" + cmpExpr.name + "`:")
                    .addSourceLine(cmpExpr);
            if (similar != null && minDist < 3) {
                err.addOffsetedSquiglyLines(cmpExpr, "Hint: Did you mean `" + similar.name + "`?");
            } else {
                err.addOffsetedSquiglyLines(cmpExpr, "");
            }
            err.addLine("Note: the record is defined with these components:")
                    .addSourceLine(defn);
            throw new Report.Error(cmpExpr, err);
        }

        var type = childDefn.accept(this, arg);

        SemAn.ofType.put(cmpExpr, type);

        // Connect with definition
        SemAn.definedAt.put(cmpExpr, childDefn);

        return type;
    }

    @Override
    public SemType visit(AstNameExpr nameExpr, Object arg) {
        var type = findVariableType(nameExpr);

        SemAn.ofType.put(nameExpr, type);
        return type;
    }

    @Override
    public SemType visit(AstPfxExpr pfxExpr, Object arg) {
        var actualType = switch (pfxExpr.oper) {
            case ADD, SUB -> checkOrThrow(pfxExpr.expr, SemIntType.type, arg);
            case NOT -> checkOrThrow(pfxExpr.expr, SemBoolType.type, arg);
            case PTR -> {
                var type = pfxExpr.expr.accept(this, arg);
                checkLValueOrThrow(pfxExpr.expr);
                yield new SemPointerType(type);
            }
        };

        SemAn.ofType.put(pfxExpr, actualType);

        return actualType;
    }

    @Override
    public SemType visit(AstSfxExpr sfxExpr, Object arg) {
        var retType = switch (sfxExpr.oper) {
            case AstSfxExpr.Oper.PTR -> {
                var type = sfxExpr.expr.accept(this, arg);

                if (type instanceof SemPointerType ptrType) {
                    // Also correct the lvalue
                    SemAn.isLVal.put(sfxExpr, true);
                    yield ptrType.baseType;
                }

                SemAn.isLVal.put(sfxExpr, false);

                var err = new ErrorAtBuilder("Dereference operator `^` can only be applied to a pointer type, but got `" + type + "`:")
                        .addSourceLine(sfxExpr);
                throw new Report.Error(sfxExpr, err);
            }
        };

        SemAn.ofType.put(sfxExpr, retType);
        return retType;
    }

    @Override
    public SemType visit(AstSizeofExpr sizeofExpr, Object arg) {
        sizeofExpr.type.accept(this, arg);

        SemAn.ofType.put(sizeofExpr, SemIntType.type);
        return SemIntType.type;
    }

    @Override
    public SemType visit(AstExprStmt exprStmt, Object arg) {
        var type = exprStmt.expr.accept(this, arg);
        SemAn.ofType.put(exprStmt, type);

        return type;
    }

    @Override
    public SemType visit(AstAssignStmt assignStmt, Object arg) {
        var typeDst = assignStmt.dst.accept(this, arg);

        checkOrThrow(assignStmt.src, typeDst, arg);
        SemAn.ofType.put(assignStmt, SemVoidType.type);

        checkLValueOrThrow(assignStmt.dst);
        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstBlockStmt blockStmt, Object arg) {
        for (final AstStmt stmt : blockStmt.stmts) {
            var type = stmt.accept(this, arg);

            if (stmt instanceof AstExprStmt exprStmt && !(exprStmt.expr instanceof AstCallExpr)) {
                if (!equiv(type, SemVoidType.type)) {
                    var err = new ErrorAtBuilder("Following expression is not a valid statement:")
                            .addSourceLine(stmt)
                            .addOffsetedSquiglyLines(stmt, "Hint: Try removing this expression.");
                    throw new Report.Error(stmt, err);
                }
            }
        }

        SemAn.ofType.put(blockStmt, SemVoidType.type);
        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstIfStmt ifStmt, Object arg) {
        checkOrThrow(ifStmt.cond, SemBoolType.type, arg);

        // We must have return in both cases, otherwise return
        // in just `if` is not (yet) valid
        // Check recursively
        ifStmt.thenStmt.accept(this, arg);
        boolean foundIfReturn = this.foundReturnType != null;
        this.foundReturnType = null;

        if (ifStmt.elseStmt != null) {
            ifStmt.elseStmt.accept(this, arg);

            if (this.foundReturnType != null && !foundIfReturn) {
                // `if` has no return, `else` has it. So we have to "check on"
                this.foundReturnType = null;
            }
        }

        SemAn.ofType.put(ifStmt, SemVoidType.type);

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstWhileStmt whileStmt, Object arg) {
        checkOrThrow(whileStmt.cond, SemBoolType.type, arg);
        whileStmt.stmt.accept(this, arg);

        SemAn.ofType.put(whileStmt, SemVoidType.type);
        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstReturnStmt retStmt, Object arg) {
        var type = retStmt.expr.accept(this, arg);
        var foundReturn = new FoundReturnType(type);
        foundReturn.stmt = retStmt;
        this.foundReturnType = foundReturn;

        // Check parent function for its return type and compare
        var expectedFnType = SemAn.ofType.get(this.currentReturningFunction);
        boolean eq = equiv(expectedFnType, type);

        if (!eq || foundReturnType == null) {
            // Error!
            functionReturnError(this.currentReturningFunction, true);
        }


        SemAn.ofType.put(retStmt, SemVoidType.type);

        return SemVoidType.type;
    }


    @Override
    public SemType visit(AstNodes<? extends AstNode> nodes, Object arg) {
        for (final AstNode node : nodes) {
            node.accept(this, arg);
        }

        return SemVoidType.type;
    }

    @Override
    public SemType visit(AstVarDefn varDefn, Object arg) {
        var type = varDefn.type.accept(this, arg);
        SemAn.ofType.put(varDefn, type);

        return type;
    }

    @Override
    public SemType visit(AstFunDefn funDefn, Object arg) {
        var fnType = funDefn.type.accept(this, arg);

        // Check return type - can't be structs or so.
        if (fnType.actualType() instanceof SemRecordType || fnType.actualType() instanceof SemUnionType) {
            var err = new ErrorAtBuilder("Functions cannot return `" + fnType + "`. Did you mean to return `^" + fnType + "`?")
                    .addSourceLine(funDefn)
                    .addOffsetedSquiglyLines(funDefn.type, "Hint: Try changing this to pointer type, `^" + funDefn.type.getText() + "`.");

            throw new Report.Error(funDefn, err);
        }

        SemAn.ofType.put(funDefn, fnType);

        for (var param : funDefn.pars) {
            param.accept(this, arg);
            checkLValueOrThrow(param);
        }

        funDefn.defns.accept(this, arg);

        if (funDefn.stmt != null) {
            this.currentReturningFunction = funDefn;
            this.foundReturnType = null;

            checkOrThrow(funDefn.stmt, SemVoidType.type, arg);

            if (foundReturnType == null) {
                // No return type was found
                if (fnType != SemVoidType.type) {
                    functionReturnError(funDefn, false);
                }
            } else {
                boolean eq = equiv(fnType, this.foundReturnType.type);
                // Throw error if not equivalent
                if (!eq) {
                    functionReturnError(funDefn, true);
                }
            }
        }

        return fnType;
    }

    private void functionReturnError(AstFunDefn funDefn, boolean isFatal) {
        var fnType = SemAn.ofType.get(funDefn);
        String type = isFatal ? "Error" : "Warning";
        var err = new ErrorAtBuilder(type, "Function `" + funDefn.name() + "` is declared to return `" + fnType + "`:")
                .addSourceLine(funDefn);
        if (foundReturnType != null && foundReturnType.stmt != null) {
            if (equiv(fnType, SemVoidType.type)) {
                err.addOffsetedSquiglyLines(funDefn.type, "Hint: try changing the return type to `" + foundReturnType.type + "`.")
                        .addLine("Actual return type is `" + foundReturnType.type + "`:")
                        .addSourceLine(foundReturnType.stmt);
            } else {
                err.addOffsetedSquiglyLines(funDefn.type, "Function signature declares`" + fnType + "` return type here.")
                        .addLine("But the actual return type is `" + foundReturnType.type + "`:")
                        .addSourceLine(foundReturnType.stmt)
                        .addOffsetedSquiglyLines(foundReturnType.stmt.expr, "Hint: Try changing this return type to `" + fnType + "`.");
            }
        } else {
            err.addOffsetedSquiglyLines(funDefn.type, "Note: This function requires returning an expression of type `" + fnType + "`.")
                    .addLine("But function end was reached without finding `return` statement:")
                    .addSourceLineEnd(funDefn)
                    .addBlankSourceLine();
        }
        final String message = err.toString();
        if (isFatal) {
            throw new Report.Error(funDefn, message);
        } else {
            Report.warning(funDefn, message);
        }
    }

    @Override
    public SemType visit(AstFunDefn.AstRefParDefn refParDefn, Object arg) {
        var type = refParDefn.type.accept(this, arg);

        if (type == SemVoidType.type) {
            var err = new ErrorAtBuilder("Reference parameter `" + refParDefn.name() + "` cannot be of type `" + type + "`:")
                    .addSourceLine(refParDefn.parent.parent)
                    .addOffsetedSquiglyLines(refParDefn, "");
            throw new Report.Error(refParDefn, err);
        }

        SemAn.ofType.put(refParDefn, type);

        return type;
    }

    @Override
    public SemType visit(AstFunDefn.AstValParDefn valParDefn, Object arg) {
        var type = valParDefn.type.accept(this, arg);

        if (type.actualType() == SemVoidType.type || type.actualType() instanceof SemRecordType || type.actualType() instanceof SemUnionType) {
            var err = new ErrorAtBuilder("Value parameter cannot be of type `" + type + "`:")
                    .addSourceLine(valParDefn.parent.parent)
                    .addOffsetedSquiglyLines(valParDefn, "");
            throw new Report.Error(valParDefn, err);
        }

        SemAn.ofType.put(valParDefn, type);

        return type;
    }

    @Override
    public SemType visit(AstArrType arrType, Object arg) {
        var elemType = arrType.elemType.accept(this, arg);

        checkOrThrow(arrType.size, SemIntType.type, arg);

        // Try to get size
        if (arrType.size instanceof AstAtomExpr atomExpr) {
            if (atomExpr.type == AstAtomExpr.Type.INT) {
                var type = new SemArrayType(elemType, Long.parseLong(atomExpr.value));

                SemAn.ofType.put(arrType, type);

                return type;
            }
        }

        throw new Report.InternalError();
    }

    @Override
    public SemType visit(AstAtomType atomType, Object arg) {
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
    public SemType visit(AstNameType nameType, Object arg) {
        var defined = SemAn.definedAt.get(nameType);
        var type = SemAn.isType.get(defined);

        if (type == null) {
            type = SemAn.ofType.get(defined);

            if (type != null) {
                var err = new ErrorAtBuilder("Name `" + nameType.name() + "` is actually a variable, but was used as type here:", nameType);
                throw new Report.Error(nameType, err);
            }

            // Ok, not, let's define it
            type = defined.accept(this, null);
            SemAn.isType.put(defined, type);
        }

        type = type.actualType();
        SemAn.isType.put(nameType, type);

        return type.actualType();
    }

    @Override
    public SemType visit(AstPtrType ptrType, Object arg) {
        var type = new SemPointerType(ptrType.baseType.accept(this, arg));
        SemAn.ofType.put(ptrType, type);

        return type;
    }

    @Override
    public SemType visit(AstStrType strType, Object arg) {
        var components = new LinkedList<SemType>();

        for (int i = 0; i < strType.cmps.size(); ++i) {
            AstNode cmp = strType.cmps.get(i);
            var type = cmp.accept(this, arg);

            components.add(type);
        }

        var type = new SemStructType(components);
        record2ast.put(type, strType);

        SemAn.ofType.put(strType, type);

        return type;
    }

    @Override
    public SemType visit(AstUniType uniType, Object arg) {
        var components = new LinkedList<SemType>();

        for (int i = 0; i < uniType.cmps.size(); ++i) {
            AstNode cmp = uniType.cmps.get(i);
            var type = cmp.accept(this, arg);
            components.add(type);
        }

        var type = new SemUnionType(components);
        record2ast.put(type, uniType);

        SemAn.ofType.put(uniType, type);

        return type;
    }

    @Override
    public SemType visit(AstRecType.AstCmpDefn cmpDefn, Object arg) {
        var type = cmpDefn.type.accept(this, arg);

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
                throw new Report.Error(node, err);
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
                throw new Report.Error(node, err);
            }

            // Ok, not, let's define it
            type = defined.accept(this, null).actualType();
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
        private final SemType type;
        private AstReturnStmt stmt;

        public FoundReturnType(SemType type) {
            this.type = type;
        }
    }
}
