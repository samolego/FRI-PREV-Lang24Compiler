package lang24.phase.imcgen;

import lang24.common.report.ErrorAtBuilder;
import lang24.common.report.Report;
import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.tree.defn.AstDefn;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.AstRecType;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.imc.code.ImcInstr;
import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.*;
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

public class ImcGenerator implements AstFullVisitor<ImcInstr, AstFunDefn> {

    @Override
    public ImcInstr visit(AstNodes<? extends AstNode> nodes, AstFunDefn parentFn) {
        var stmtList = new LinkedList<ImcStmt>();

        for (var astNode : nodes) {
            var stmt = astNode.accept(this, parentFn);
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
    public ImcInstr visit(AstFunDefn funDefn, AstFunDefn parentFn) {
        var entryLabel = new MemLabel();
        var exitLabel = new MemLabel();

        ImcGen.entryLabel.put(funDefn, entryLabel);
        ImcGen.exitLabel.put(funDefn, exitLabel);

        funDefn.pars.accept(this, parentFn);
        funDefn.defns.accept(this, parentFn);

        if (funDefn.stmt != null) {
            funDefn.stmt.accept(this, funDefn);
        }

        return null;
    }

    // needed?
    /*@Override
    public ImcInstr visit(AstVarDefn varDefn, AstFunDefn arg) {
        // todo ?? new ImcCONST(varDefn)
        return null;
    }

    @Override
    public ImcInstr visit(AstFunDefn.AstRefParDefn refParDefn, AstFunDefn arg) {
        refParDefn.
        return AstFullVisitor.super.visit(refParDefn, arg);
    }

    @Override
    public ImcInstr visit(AstFunDefn.AstValParDefn valParDefn, AstFunDefn arg) {
        return AstFullVisitor.super.visit(valParDefn, arg);
    }*/

    @Override
    public ImcInstr visit(AstArrExpr arrExpr, AstFunDefn parentFn) {
        var array = (ImcExpr) arrExpr.arr.accept(this, parentFn);

        // Remove wrapper MEM if it exists
        if (array instanceof ImcMEM mem) {
            array = mem.addr;
        }

        var idx = (ImcExpr) arrExpr.idx.accept(this, parentFn);

        // Get size of the array type
        var arrType = (SemArrayType) SemAn.ofType.get(arrExpr.arr);
        long elemTypeSize = MemEvaluator.getSizeInBytes(arrType.elemType);

        // Multiply index by size of the type to get offset
        var offset = new ImcBINOP(ImcBINOP.Oper.MUL, idx, new ImcCONST(elemTypeSize));

        // Add offset to the array to get the memory access address
        var binOp = new ImcBINOP(ImcBINOP.Oper.ADD, array, offset);

        var memAcc = new ImcMEM(binOp);
        ImcGen.exprImc.put(arrExpr, memAcc);

        return memAcc;
    }


    /**
     * Constant expressions.
     *
     * @param atomExpr The atom expression of AST.
     * @param parentFn The parent function definition.
     * @return The intermediate code instruction.
     */
    @Override
    public ImcInstr visit(AstAtomExpr atomExpr, AstFunDefn parentFn) {
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
                    yield new ImcCONST(valueStr.charAt(2));
                }

                if (length == 3) {
                    // \ hex hex
                    long num = Long.parseLong(valueStr.substring(2, length), 16);
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
                            .addOffsetedSquiglyLines(atomExpr, "Should be between " + Long.MIN_VALUE + " and " + Long.MAX_VALUE + ".");
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
    public ImcInstr visit(AstBinExpr binExpr, AstFunDefn parentFn) {
        // Get child expressions
        var fstExpr = (ImcExpr) binExpr.fstExpr.accept(this, parentFn);
        var sndExpr = (ImcExpr) binExpr.sndExpr.accept(this, parentFn);

        // Define operator
        final var oper = switch (binExpr.oper) {
            case ADD -> ImcBINOP.Oper.ADD;
            case SUB -> ImcBINOP.Oper.SUB;
            case MUL -> ImcBINOP.Oper.MUL;
            case DIV -> ImcBINOP.Oper.DIV;
            case MOD -> ImcBINOP.Oper.MOD;
            case AND -> ImcBINOP.Oper.AND;
            case OR -> ImcBINOP.Oper.OR;
            case EQU -> ImcBINOP.Oper.EQU;
            case NEQ -> ImcBINOP.Oper.NEQ;
            case LTH -> ImcBINOP.Oper.LTH;
            case GTH -> ImcBINOP.Oper.GTH;
            case LEQ -> ImcBINOP.Oper.LEQ;
            case GEQ -> ImcBINOP.Oper.GEQ;
        };

        var binImc = new ImcBINOP(oper, fstExpr, sndExpr);
        ImcGen.exprImc.put(binExpr, binImc);

        return binImc;
    }

    /**
     * Function call expressions.
     *
     * @param callExpr The call expression.
     * @param parentFn The parent function definition.
     * @return The intermediate code instruction.
     */
    @Override
    public ImcInstr visit(AstCallExpr callExpr, AstFunDefn parentFn) {
        // Fill argument expressions
        var args = new LinkedList<ImcExpr>();
        for (AstExpr argExpr : callExpr.args) {
            var imcExpr = (ImcExpr) argExpr.accept(this, parentFn);
            args.add(imcExpr);
        }

        // Original function definition
        var fnDefn = (AstFunDefn) SemAn.definedAt.get(callExpr);

        // Get memory label
        var frame = Memory.frames.get(fnDefn);
        var label = frame == null
                ? new MemLabel(fnDefn.name())  // Prototype
                : frame.label;  // Function definition in the same file

        var offsets = new LinkedList<Long>();
        // Get offsets of arguments
        for (var fnArg : fnDefn.pars) {
            var access = Memory.parAccesses.get(fnArg);
            offsets.add(access.offset);
        }

        var callImc = new ImcCALL(label, offsets, args);
        ImcGen.exprImc.put(callExpr, callImc);

        return callImc;
    }

    @Override
    public ImcInstr visit(AstCastExpr castExpr, AstFunDefn parentFn) {
        var exprImc = (ImcExpr) castExpr.expr.accept(this, parentFn);

        var expr = SemAn.isType.get(castExpr.type) instanceof SemCharType
                ? new ImcBINOP(ImcBINOP.Oper.AND, exprImc, new ImcCONST(0x0FFL))  // If char is cast, we must mod it with 256
                : exprImc;  // Otherwise, use the written value

        ImcGen.exprImc.put(castExpr, expr);

        return expr;
    }

    @Override
    public ImcInstr visit(AstCmpExpr cmpExpr, AstFunDefn parentFn) {
        var leftExpr = (ImcExpr) cmpExpr.expr.accept(this, parentFn);

        if (leftExpr instanceof ImcMEM mem) {
            leftExpr = mem.addr;
        }

        // Get component ast definition
        var cmpDefn = (AstRecType.AstCmpDefn) SemAn.definedAt.get(cmpExpr);
        // Find memory access
        var memAccess = Memory.cmpAccesses.get(cmpDefn);

        // Our result is at leftExpr + offset
        var binImc = new ImcBINOP(ImcBINOP.Oper.ADD, leftExpr, new ImcCONST(memAccess.offset));
        ImcGen.exprImc.put(cmpExpr, binImc);

        return binImc;
    }

    // todo - test this, ex7 rule
    @Override
    public ImcInstr visit(AstNameExpr nameExpr, AstFunDefn parentFn) {
        var defn = SemAn.definedAt.get(nameExpr);

        var access = switch (defn) {
            case AstVarDefn varDefn -> Memory.varAccesses.get(varDefn);
            case AstFunDefn.AstParDefn parDefn -> Memory.parAccesses.get(parDefn);
            default -> throw new Report.InternalError();
        };

        // Inner memory access (will be used in the MEM instruction)
        var imc = switch (access) {
            case MemAbsAccess absAccess -> {
                var label = absAccess.label;
                yield new ImcNAME(label);
            }
            case MemRelAccess relAccess -> {
                var frame = Memory.frames.get(parentFn);

                // Check if inside current function
                if (relAccess.depth == frame.depth) {
                    // Inside current function
                    var offset = new ImcCONST(relAccess.offset);
                    yield new ImcBINOP(ImcBINOP.Oper.ADD, new ImcTEMP(frame.FP), offset);
                } else {
                    // Go through static link(s)
                    long diff = relAccess.depth - frame.depth;

                    ImcExpr temp = new ImcTEMP(frame.FP);
                    for (int i = 0; i < diff; i++) {
                        temp = new ImcMEM(temp);
                    }

                    var offset = new ImcCONST(relAccess.offset);
                    yield new ImcBINOP(ImcBINOP.Oper.ADD, temp, offset);
                }
            }
            default -> throw new Report.InternalError();
        };

        // Memory access instruction
        var memInc = new ImcMEM(imc);
        ImcGen.exprImc.put(nameExpr, memInc);

        return memInc;
    }

    @Override
    public ImcInstr visit(AstPfxExpr pfxExpr, AstFunDefn parentFn) {
        var expr = (ImcExpr) pfxExpr.expr.accept(this, parentFn);

        var imc = switch (pfxExpr.oper) {
            case ADD -> expr;
            case SUB -> new ImcUNOP(ImcUNOP.Oper.NEG, expr);
            case NOT -> new ImcUNOP(ImcUNOP.Oper.NOT, expr);
            case PTR -> {
                if (expr instanceof ImcMEM) {
                    yield expr;
                }
                yield new ImcMEM(expr);
            }
        };

        ImcGen.exprImc.put(pfxExpr, imc);
        return imc;
    }

    // Todo A5 rule
    @Override
    public ImcInstr visit(AstSfxExpr sfxExpr, AstFunDefn parentFn) {
        var expr = (ImcExpr) sfxExpr.expr.accept(this, parentFn);

        var imc = switch (sfxExpr.oper) {
            case PTR -> // What to do here? Todo
                    new ImcMEM(expr);
        };

        ImcGen.exprImc.put(sfxExpr, imc);
        return imc;
    }

    @Override
    public ImcInstr visit(AstSizeofExpr sizeofExpr, AstFunDefn parentFn) {
        var type = SemAn.isType.get(sizeofExpr.type);
        var size = MemEvaluator.getSizeInBytes(type);

        var imc = new ImcCONST(size);
        ImcGen.exprImc.put(sizeofExpr, imc);

        return imc;
    }

    @Override
    public ImcInstr visit(AstAssignStmt assignStmt, AstFunDefn parentFn) {
        var dstExpr = (ImcExpr) assignStmt.dst.accept(this, parentFn);
        var srcExpr = (ImcExpr) assignStmt.src.accept(this, parentFn);

        // todo - how to handle st2 rule?
        var assignImc = new ImcMOVE(dstExpr, srcExpr);
        ImcGen.stmtImc.put(assignStmt, assignImc);

        return assignImc;
    }

    @Override
    public ImcInstr visit(AstBlockStmt blockStmt, AstFunDefn parentFn) {
        var stmts = new LinkedList<ImcStmt>();

        for (var stmt : blockStmt.stmts) {
            var child = stmt.accept(this, parentFn);
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
    public ImcInstr visit(AstExprStmt exprStmt, AstFunDefn parentFn) {
        var expr = exprStmt.expr.accept(this, parentFn);

        var imcStmt = new ImcESTMT((ImcExpr) expr);
        ImcGen.stmtImc.put(exprStmt, imcStmt);

        return imcStmt;
    }

    @Override
    public ImcInstr visit(AstIfStmt ifStmt, AstFunDefn parentFn) {
        var cond = (ImcExpr) ifStmt.cond.accept(this, parentFn);
        var thenStmt = (ImcStmt) ifStmt.thenStmt.accept(this, parentFn);

        ImcStmt elseStmt = null;
        if (ifStmt.elseStmt != null) {
            elseStmt = (ImcStmt) ifStmt.elseStmt.accept(this, parentFn);
        }

        var stmts = new LinkedList<ImcStmt>();
        var thenLabel = new MemLabel();
        var elseLabel = new MemLabel();

        var cjump = new ImcCJUMP(cond, thenLabel, elseLabel);
        stmts.add(cjump);

        // If-then part
        stmts.add(new ImcLABEL(thenLabel));
        stmts.add(thenStmt);

        // Else part
        stmts.add(new ImcJUMP(elseLabel));
        if (elseStmt != null) {
            stmts.add(elseStmt);
        }

        var imcStmt = new ImcSTMTS(stmts);
        ImcGen.stmtImc.put(ifStmt, imcStmt);

        return imcStmt;
    }

    @Override
    public ImcInstr visit(AstReturnStmt retStmt, AstFunDefn parentFn) {
        var type = SemAn.ofType.get(parentFn);
        var exitLabel = ImcGen.exitLabel.get(parentFn);

        // Jump to the exit label
        var jump = new ImcJUMP(exitLabel);

        if (type == SemVoidType.type) {
            // Return statement in void function
            ImcGen.stmtImc.put(retStmt, jump);
            return jump;
        }

        // Non-void
        var expr = (ImcExpr) retStmt.expr.accept(this, parentFn);

        // Move the expression to the return value
        var frame = Memory.frames.get(parentFn);

        // Write the return value to the stack
        var move = new ImcMOVE(new ImcTEMP(frame.RV), expr);
        var imcStmt = new ImcSTMTS(List.of(move, jump));
        ImcGen.stmtImc.put(retStmt, imcStmt);

        return imcStmt;
    }

    @Override
    public ImcInstr visit(AstWhileStmt whileStmt, AstFunDefn parentFn) {
        var cond = (ImcExpr) whileStmt.cond.accept(this, parentFn);

        var stmt = (ImcStmt) whileStmt.stmt.accept(this, parentFn);

        var stmts = new LinkedList<ImcStmt>();
        var loopLabel = new MemLabel();
        var exitLabel = new MemLabel();

        stmts.add(new ImcLABEL(loopLabel));
        stmts.add(new ImcCJUMP(cond, loopLabel, exitLabel));
        stmts.add(stmt);
        stmts.add(new ImcLABEL(exitLabel));

        var imcStmt = new ImcSTMTS(stmts);
        ImcGen.stmtImc.put(whileStmt, imcStmt);

        return imcStmt;
    }
}
