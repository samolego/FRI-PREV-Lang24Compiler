package lang24.phase.imcgen;

import lang24.common.report.ErrorAtBuilder;
import lang24.common.report.Report;
import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstFunDefn.AstRefParDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.AstArrExpr;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.tree.expr.AstBinExpr;
import lang24.data.ast.tree.expr.AstCallExpr;
import lang24.data.ast.tree.expr.AstCastExpr;
import lang24.data.ast.tree.expr.AstCmpExpr;
import lang24.data.ast.tree.expr.AstNameExpr;
import lang24.data.ast.tree.expr.AstPfxExpr;
import lang24.data.ast.tree.expr.AstSfxExpr;
import lang24.data.ast.tree.expr.AstSizeofExpr;
import lang24.data.ast.tree.stmt.AstAssignStmt;
import lang24.data.ast.tree.stmt.AstBlockStmt;
import lang24.data.ast.tree.stmt.AstExprStmt;
import lang24.data.ast.tree.stmt.AstIfStmt;
import lang24.data.ast.tree.stmt.AstReturnStmt;
import lang24.data.ast.tree.stmt.AstWhileStmt;
import lang24.data.ast.tree.type.AstRecType;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.imc.code.ImcInstr;
import lang24.data.imc.code.expr.ImcBINOP;
import lang24.data.imc.code.expr.ImcBINOP.Oper;
import lang24.data.imc.code.expr.ImcCALL;
import lang24.data.imc.code.expr.ImcCONST;
import lang24.data.imc.code.expr.ImcExpr;
import lang24.data.imc.code.expr.ImcMEM;
import lang24.data.imc.code.expr.ImcNAME;
import lang24.data.imc.code.expr.ImcTEMP;
import lang24.data.imc.code.expr.ImcUNOP;
import lang24.data.imc.code.stmt.ImcCJUMP;
import lang24.data.imc.code.stmt.ImcESTMT;
import lang24.data.imc.code.stmt.ImcJUMP;
import lang24.data.imc.code.stmt.ImcLABEL;
import lang24.data.imc.code.stmt.ImcMOVE;
import lang24.data.imc.code.stmt.ImcSTMTS;
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.mem.MemAbsAccess;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemRelAccess;
import lang24.data.type.SemArrayType;
import lang24.data.type.SemCharType;
import lang24.data.type.SemVoidType;
import lang24.phase.memory.MemEvaluator;
import lang24.phase.memory.Memory;
import lang24.phase.seman.SemAn;

import java.util.LinkedList;
import java.util.List;

/**
 * Na novo spisan imc generator - od prejšnjega tedna ostaja v {@link ImcGenerator}, toliko, da se vidi razlika med popravki.
 */
public class IG2 implements AstFullVisitor<ImcInstr, AstFunDefn> {

    private static final ImcExpr STATIC_LINK_DUMMY = new ImcCONST(541);

    @Override
    public ImcInstr visit(AstNodes<? extends AstNode> nodes, AstFunDefn currentFn) {
        var stmtList = new LinkedList<ImcStmt>();

        for (var astNode : nodes) {
            var stmt = astNode.accept(this, currentFn);
            switch (stmt) {
                case ImcExpr expr -> stmtList.add(new ImcESTMT(expr));
                case ImcStmt imcStmt -> stmtList.add(imcStmt);
                case null -> {
                    assert astNode instanceof AstDefn : "Got null for non-defn!";
                }
                default -> throw new Report.InternalError();
            }
        }

        return new ImcSTMTS(stmtList);
    }

    @Override
    public ImcInstr visit(AstFunDefn funDefn, AstFunDefn currentFn) {
        var entryLabel = new MemLabel();
        var exitLabel = new MemLabel();

        ImcGen.entryLabel.put(funDefn, entryLabel);
        ImcGen.exitLabel.put(funDefn, exitLabel);

        funDefn.pars.accept(this, funDefn);
        funDefn.defns.accept(this, funDefn);

        if (funDefn.stmt != null) {
            // Not a prototype
            var stmts = funDefn.stmt.accept(this, funDefn);

            // Force set the RV to 0 if there is no return statement
            if (!funDefn.hasReturnStmt) {
                // Move the expression to the return value
                var frame = Memory.frames.get(funDefn);
                var dummyReturn = new ImcCONST(0);

                var jump = new ImcJUMP(exitLabel);
                var move = new ImcMOVE(new ImcTEMP(frame.RV), dummyReturn);

                // New statement list for the function
                var newStmts = switch (stmts) {
                    case ImcSTMTS stmtsList -> {

                        stmtsList.stmts.add(move);
                        stmtsList.stmts.add(jump);

                        yield stmtsList;
                    }
                    // Single expression
                    case ImcExpr expr -> new ImcSTMTS(List.of(new ImcESTMT(expr), move, jump));
                    case ImcStmt stmt -> new ImcSTMTS(List.of(stmt, move, jump));
                    case null, default -> throw new Report.InternalError();
                };

                // Overwrite original
                ImcGen.stmtImc.put(funDefn.stmt, newStmts);
            }
        }

        return null;
    }

    @Override
    public ImcInstr visit(AstArrExpr arrExpr, AstFunDefn currentFn) {
        var array = (ImcExpr) arrExpr.arr.accept(this, currentFn);

        // Remove wrapper MEM if it exists
        if (array instanceof ImcMEM mem) {
            array = mem.addr;
        }

        var idx = (ImcExpr) arrExpr.idx.accept(this, currentFn);

        // Get size of the array type
        var arrType = (SemArrayType) SemAn.ofType.get(arrExpr.arr);
        long elemTypeSize = MemEvaluator.getSizeInBytes(arrType.elemType);

        // Multiply index by size of the type to get offset
        var offset = new ImcBINOP(Oper.MUL, idx, new ImcCONST(elemTypeSize));

        // Add offset to the array to get the memory access address
        var binOp = new ImcBINOP(Oper.ADD, array, offset);

        var memAcc = new ImcMEM(binOp);
        ImcGen.exprImc.put(arrExpr, memAcc);

        return memAcc;
    }


    /**
     * Constant expressions.
     *
     * @param atomExpr  The atom expression of AST.
     * @param currentFn The parent function definition.
     * @return The intermediate code instruction.
     */
    @Override
    public ImcInstr visit(AstAtomExpr atomExpr, AstFunDefn currentFn) {
        var valueStr = atomExpr.value;

        // Get constant value
        var constImc = switch (atomExpr.type) {
            case BOOL -> new ImcCONST(Boolean.parseBoolean(valueStr) ? 1L : 0L);
            case CHAR -> {
                int length = valueStr.length() - 2;  // -2 = remove quotes
                if (length == 1) {
                    yield new ImcCONST(valueStr.charAt(1));  // Get ascii value of the character
                }
                if (length == 2) {
                    // Either \\ or \n or \'
                    if (valueStr.charAt(2) != 'n') {
                        // \\ or \'
                        yield new ImcCONST(valueStr.charAt(2));
                    } else {
                        // \n
                        yield new ImcCONST('\n');
                    }
                }

                if (length == 3) {
                    // \ hex hex
                    long num = Long.parseLong(valueStr.substring(2, length + 1), 16);
                    yield new ImcCONST(num);
                }

                throw new Report.InternalError();
            }
            case INT -> {
                // Remove leading zeros
                valueStr = valueStr.replaceFirst("^0+(?!$)", "");
                try {
                    yield new ImcCONST(Long.parseLong(valueStr));
                } catch (NumberFormatException e) {
                    var err = new ErrorAtBuilder("Number out of range: `" + valueStr + "`.")
                            .addSourceLine(atomExpr)
                            .addUnderlineWrongChar(atomExpr, "Should be between " + Long.MIN_VALUE + " and " + Long.MAX_VALUE + ".");
                    throw new Report.Error(atomExpr, err);
                }
            }
            case VOID, PTR -> new ImcCONST(0L);
            case STR -> {
                var stringMem = Memory.strings.get(atomExpr);
                yield new ImcNAME(stringMem.label);
            }
        };

        ImcGen.exprImc.put(atomExpr, constImc);

        return constImc;
    }

    @Override
    public ImcInstr visit(AstBinExpr binExpr, AstFunDefn currentFn) {
        // Get child expressions
        var fstExpr = (ImcExpr) binExpr.fstExpr.accept(this, currentFn);
        var sndExpr = (ImcExpr) binExpr.sndExpr.accept(this, currentFn);

        // Define operator
        final var oper = switch (binExpr.oper) {
            case ADD -> Oper.ADD;
            case SUB -> Oper.SUB;
            case MUL -> Oper.MUL;
            case DIV -> Oper.DIV;
            case MOD -> Oper.MOD;
            case AND -> Oper.AND;
            case OR -> Oper.OR;
            case EQU -> Oper.EQU;
            case NEQ -> Oper.NEQ;
            case LTH -> Oper.LTH;
            case GTH -> Oper.GTH;
            case LEQ -> Oper.LEQ;
            case GEQ -> Oper.GEQ;
        };

        var binImc = new ImcBINOP(oper, fstExpr, sndExpr);
        ImcGen.exprImc.put(binExpr, binImc);

        return binImc;
    }

    /**
     * Function call expressions.
     *
     * @param callExpr  The call expression.
     * @param currentFn The parent function definition.
     * @return The intermediate code instruction.
     */
    @Override
    public ImcInstr visit(AstCallExpr callExpr, AstFunDefn currentFn) {
        // Original function definition
        var callFnDefn = (AstFunDefn) SemAn.definedAt.get(callExpr);

        // Get memory label
        var calledFrame = Memory.frames.get(callFnDefn);
        var label = calledFrame == null
                ? new MemLabel(callFnDefn.name())  // Prototype
                : calledFrame.label;  // Function definition in the same file


        // Static link (or dummy for prototypes)
        var currentFrame = Memory.frames.get(currentFn);
        ImcExpr sl = IG2.STATIC_LINK_DUMMY;
        if (calledFrame != null && calledFrame.depth > 0L) {
            long depthDiff = currentFrame.depth - calledFrame.depth + 1;
            // Need to go through static links depthDiff times
            sl = new ImcTEMP(currentFrame.FP);
            for (int i = 0; i < depthDiff; i++) {
                sl = new ImcMEM(sl);
            }
        }

        // Fill argument expressions
        var args = new LinkedList<ImcExpr>();
        args.add(sl);


        var offsets = new LinkedList<Long>();
        // Include static link in the offsets
        offsets.add(0L);

        var callArgsIter = callExpr.args.iterator();
        var fnParDefnsIter = callFnDefn.pars.iterator();

        while (callArgsIter.hasNext() && fnParDefnsIter.hasNext()) {
            var argExpr = callArgsIter.next();
            var fnParDefn = fnParDefnsIter.next();

            // Get argument expression
            var imcExpr = (ImcExpr) argExpr.accept(this, currentFn);

            // Warning! If parameter is a reference, we must pass the address of the argument
            if (fnParDefn instanceof AstRefParDefn) {
                if (imcExpr instanceof ImcMEM mem) {
                    imcExpr = mem.addr;
                } else {
                    // Expected a memory access, got something else
                    throw new Report.InternalError();
                }
            }

            args.add(imcExpr);

            // Get offset of argument
            var access = Memory.parAccesses.get(fnParDefn);
            offsets.add(access.offset);
        }


        var callImc = new ImcCALL(label, offsets, args);
        ImcGen.exprImc.put(callExpr, callImc);

        return callImc;
    }

    @Override
    public ImcInstr visit(AstCastExpr castExpr, AstFunDefn currentFn) {
        var exprImc = (ImcExpr) castExpr.expr.accept(this, currentFn);

        var expr = SemAn.isType.get(castExpr.type) instanceof SemCharType
                ? new ImcBINOP(Oper.MOD, exprImc, new ImcCONST(0x0FFL))  // If char is cast, we must mod it with 256
                : exprImc;  // Otherwise, use the written value

        ImcGen.exprImc.put(castExpr, expr);

        return expr;
    }

    @Override
    public ImcInstr visit(AstCmpExpr cmpExpr, AstFunDefn currentFn) {
        var leftExpr = (ImcExpr) cmpExpr.expr.accept(this, currentFn);

        if (leftExpr instanceof ImcMEM mem) {
            leftExpr = mem.addr;
        }

        // Get component ast definition
        var cmpDefn = (AstRecType.AstCmpDefn) SemAn.definedAt.get(cmpExpr);
        // Find memory access
        var memAccess = Memory.cmpAccesses.get(cmpDefn);

        // Our result is at leftExpr + offset
        var binImc = new ImcBINOP(Oper.ADD, leftExpr, new ImcCONST(memAccess.offset));
        ImcGen.exprImc.put(cmpExpr, binImc);

        return binImc;
    }

    // ex7 rule
    @Override
    public ImcInstr visit(AstNameExpr nameExpr, AstFunDefn currentFn) {
        var defn = SemAn.definedAt.get(nameExpr);

        var access = switch (defn) {
            case AstVarDefn varDefn -> Memory.varAccesses.get(varDefn);
            case AstFunDefn.AstParDefn parDefn -> Memory.parAccesses.get(parDefn);
            default -> throw new Report.InternalError();
        };

        // Address of the parameter
        var address = switch (access) {
            case MemAbsAccess absAccess -> {
                var label = absAccess.label;
                yield new ImcNAME(label);
            }
            case MemRelAccess relAccess -> {
                var currentFrame = Memory.frames.get(currentFn);

                long depthDiff = currentFrame.depth - relAccess.depth;
                ImcExpr temp = new ImcTEMP(currentFrame.FP);

                // Go through static link(s) if needed
                for (int i = 0; i < depthDiff; i++) {
                    temp = new ImcMEM(temp);
                }

                var offset = new ImcCONST(relAccess.offset);
                yield new ImcBINOP(Oper.ADD, temp, offset);
            }
            default -> throw new Report.InternalError();
        };

        // Memory access instruction
        var memImc = new ImcMEM(address);

        // If defn is reference, apply one more MEM over it
        if (defn instanceof AstRefParDefn) {
            memImc = new ImcMEM(memImc);
        }

        ImcGen.exprImc.put(nameExpr, memImc);

        return memImc;
    }

    @Override
    public ImcInstr visit(AstPfxExpr pfxExpr, AstFunDefn currentFn) {
        var expr = (ImcExpr) pfxExpr.expr.accept(this, currentFn);

        var imc = switch (pfxExpr.oper) {
            case ADD -> expr;
            case SUB -> new ImcUNOP(ImcUNOP.Oper.NEG, expr);
            case NOT -> new ImcUNOP(ImcUNOP.Oper.NOT, expr);
            case PTR -> {
                if (expr instanceof ImcMEM mem) {
                    yield mem.addr;
                }
                throw new Report.InternalError();
            }
        };

        ImcGen.exprImc.put(pfxExpr, imc);
        return imc;
    }

    // A5 rule
    @Override
    public ImcInstr visit(AstSfxExpr sfxExpr, AstFunDefn currentFn) {
        var expr = (ImcExpr) sfxExpr.expr.accept(this, currentFn);

        ImcExpr imc = switch (sfxExpr.oper) {
            case PTR -> new ImcMEM(expr);
        };

        ImcGen.exprImc.put(sfxExpr, imc);
        return imc;
    }

    @Override
    public ImcInstr visit(AstSizeofExpr sizeofExpr, AstFunDefn currentFn) {
        var type = SemAn.isType.get(sizeofExpr.type);
        var size = MemEvaluator.getSizeInBytes(type);

        var imc = new ImcCONST(size);
        ImcGen.exprImc.put(sizeofExpr, imc);

        return imc;
    }

    @Override
    public ImcInstr visit(AstAssignStmt assignStmt, AstFunDefn currentFn) {
        var dstExpr = (ImcExpr) assignStmt.dst.accept(this, currentFn);
        var srcExpr = (ImcExpr) assignStmt.src.accept(this, currentFn);

        // todo - how to handle st2 rule?
        var assignImc = new ImcMOVE(dstExpr, srcExpr);
        ImcGen.stmtImc.put(assignStmt, assignImc);

        return assignImc;
    }

    @Override
    public ImcInstr visit(AstBlockStmt blockStmt, AstFunDefn currentFn) {
        var stmts = new LinkedList<ImcStmt>();

        for (var stmt : blockStmt.stmts) {
            var child = stmt.accept(this, currentFn);
            if (child instanceof ImcExpr expr) {
                stmts.add(new ImcESTMT(expr));
            } else {
                stmts.add((ImcStmt) child);
            }
        }

        var imcStmt = new ImcSTMTS(stmts);
        ImcGen.stmtImc.put(blockStmt, imcStmt);

        return imcStmt;
    }

    @Override
    public ImcInstr visit(AstExprStmt exprStmt, AstFunDefn currentFn) {
        var expr = exprStmt.expr.accept(this, currentFn);

        var imcStmt = new ImcESTMT((ImcExpr) expr);
        ImcGen.stmtImc.put(exprStmt, imcStmt);

        return imcStmt;
    }

    @Override
    public ImcInstr visit(AstIfStmt ifStmt, AstFunDefn currentFn) {
        var cond = (ImcExpr) ifStmt.cond.accept(this, currentFn);
        // Check if condition is binary expression - if so, make it short-circuit

        var thenStmt = (ImcStmt) ifStmt.thenStmt.accept(this, currentFn);

        // Whether it has else part
        boolean hasElseStatements = false;
        ImcStmt elseStmt = null;
        if (ifStmt.elseStmt != null) {
            elseStmt = (ImcStmt) ifStmt.elseStmt.accept(this, currentFn);
            hasElseStatements = true;
        }

        var stmts = new LinkedList<ImcStmt>();

        // Labels
        var thenLabel = new MemLabel();
        var elseLabel = new MemLabel();
        var exitLabel = new MemLabel();

        // Jump to exit and exit label
        var imcExitLbl = new ImcLABEL(exitLabel);
        var jumpToExit = new ImcJUMP(exitLabel);

        // If no else part, jump to exit directly

        if (cond instanceof ImcBINOP bnp) {
            var left = (ImcExpr) bnp.fstExpr;
            var right = (ImcExpr) bnp.sndExpr;

            // Short circuit and
            if (bnp.oper == Oper.AND) {
                // Short-circuit AND
                // Labels
                var andLabel = new MemLabel();

                // If no else part, jump to exit directly
                var cjump = new ImcCJUMP(left, andLabel, exitLabel);
                stmts.add(cjump);

                // And part
                stmts.add(new ImcLABEL(andLabel));
                stmts.add(new ImcCJUMP(right, thenLabel, hasElseStatements ? elseLabel : exitLabel));
            } else if (bnp.oper == Oper.OR) {
                var orLbl = new MemLabel();
                var orLblInstr = new ImcLABEL(orLbl);
                // Short circuit OR
                var orJump1 = new ImcCJUMP(left, thenLabel, orLbl);
                stmts.add(orJump1);
                stmts.add(orLblInstr);

                var orJump2 = new ImcCJUMP(right, thenLabel, hasElseStatements ? elseLabel : exitLabel);
                stmts.add(orJump2);
            }
        } else {
            var cjump = new ImcCJUMP(cond, thenLabel, hasElseStatements ? elseLabel : exitLabel);
            stmts.add(cjump);
        }

        // Else part
        if (hasElseStatements) {
            stmts.add(new ImcLABEL(elseLabel));
            stmts.add(elseStmt);
            stmts.add(jumpToExit);
        }

        // If-then part
        stmts.add(new ImcLABEL(thenLabel));
        stmts.add(thenStmt);
        stmts.add(jumpToExit);

        // Add exit label
        stmts.add(imcExitLbl);

        var imcStmt = new ImcSTMTS(stmts);
        ImcGen.stmtImc.put(ifStmt, imcStmt);

        return imcStmt;
    }

    @Override
    public ImcInstr visit(AstReturnStmt retStmt, AstFunDefn currentFn) {
        var type = SemAn.ofType.get(currentFn);
        var exitLabel = ImcGen.exitLabel.get(currentFn);

        // Jump to the exit label
        var jump = new ImcJUMP(exitLabel);

        /*if (type == SemVoidType.type) {
            // Return statement in void function
            ImcGen.stmtImc.put(retStmt, jump);
            return jump;
        }*/

        // To keep interpreter happy, we also make void functions return <something>
        var expr = type == SemVoidType.type ? new ImcCONST(0) : (ImcExpr) retStmt.expr.accept(this, currentFn);

        // Move the expression to the return value
        var frame = Memory.frames.get(currentFn);

        // Write the return value to the stack
        var move = new ImcMOVE(new ImcTEMP(frame.RV), expr);
        var imcStmt = new ImcSTMTS(List.of(move, jump));
        ImcGen.stmtImc.put(retStmt, imcStmt);

        return imcStmt;
    }

    @Override
    public ImcInstr visit(AstWhileStmt whileStmt, AstFunDefn currentFn) {
        var condExpr = (ImcExpr) whileStmt.cond.accept(this, currentFn);
        var stmtExpr = (ImcStmt) whileStmt.stmt.accept(this, currentFn);

        var stmts = new LinkedList<ImcStmt>();
        var loopLabel = new MemLabel();
        var bodyLabel = new MemLabel();
        var exitLabel = new MemLabel();

        stmts.add(new ImcLABEL(loopLabel));
        stmts.add(new ImcCJUMP(condExpr, bodyLabel, exitLabel));
        stmts.add(new ImcLABEL(bodyLabel));
        stmts.add(stmtExpr);
        stmts.add(new ImcJUMP(loopLabel));
        stmts.add(new ImcLABEL(exitLabel));

        var imcStmt = new ImcSTMTS(stmts);
        ImcGen.stmtImc.put(whileStmt, imcStmt);

        return imcStmt;
    }
}
