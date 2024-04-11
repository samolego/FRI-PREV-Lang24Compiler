package lang24.phase.imcgen;

import lang24.common.report.ErrorAtBuilder;
import lang24.common.report.Report;
import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstTypDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.ast.visitor.AstVisitor;
import lang24.data.imc.code.ImcInstr;
import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.ImcMOVE;
import lang24.data.imc.code.stmt.ImcSTMTS;
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.mem.MemAbsAccess;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemRelAccess;
import lang24.data.type.SemCharType;
import lang24.phase.memory.Memory;
import lang24.phase.seman.SemAn;

import java.util.LinkedList;

public class ImcGenerator implements AstVisitor<ImcInstr, Object> {

    @Override
    public ImcInstr visit(AstNodes<? extends AstNode> nodes, Object arg) {
        var stmtList = new LinkedList<ImcStmt>();

        for (var astNode : nodes) {
            var stmt = astNode.accept(this, arg);
            if (!(stmt instanceof ImcStmt)) {
                System.err.println("Expected ImcStmt, got " + stmt.getClass().getSimpleName() + " " + astNode + " " + stmt);
                //throw new Report.InternalError();
            } else {
                stmtList.add((ImcStmt) stmt);
            }
        }

        return new ImcSTMTS(stmtList);
    }

    @Override
    public ImcInstr visit(AstTypDefn typDefn, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstVarDefn varDefn, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstFunDefn funDefn, Object arg) {
        // put to ImcGen.entryLabel & ImcGen.exitLabel todo
        return null;
    }

    @Override
    public ImcInstr visit(AstFunDefn.AstRefParDefn refParDefn, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstFunDefn.AstValParDefn valParDefn, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstArrExpr arrExpr, Object arg) {
        return null;
    }


    @Override
    public ImcInstr visit(AstAtomExpr atomExpr, Object arg) {
        var valueStr = atomExpr.value;
        long value = switch (atomExpr.type) {
            case BOOL -> Boolean.parseBoolean(valueStr) ? 1L : 0L;
            case CHAR -> (long) valueStr.charAt(0);  // Get ascii value of the character
            case INT -> {
                // Remove leading zeros
                valueStr = valueStr.replaceFirst("^0+(?!$)", "");
                try {
                    yield Long.parseLong(valueStr);
                } catch (NumberFormatException e) {
                    var err = new ErrorAtBuilder("Number out of range: `" + valueStr + "`.")
                            .addSourceLine(atomExpr)
                            .addOffsetedSquiglyLines(atomExpr, "Should be between " + Long.MIN_VALUE + " and " + Long.MAX_VALUE + ".");
                    throw new Report.Error(atomExpr, err);
                }
            }
            case VOID, PTR, STR -> 0L;
        };

        var constImc = new ImcCONST(value);
        ImcGen.exprImc.put(atomExpr, constImc);

        return constImc;
    }

    @Override
    public ImcInstr visit(AstBinExpr binExpr, Object arg) {
        var fstImc = binExpr.fstExpr.accept(this, arg);
        var sndImc = binExpr.sndExpr.accept(this, arg);

        if (!(fstImc instanceof ImcExpr fstExpr) || !(sndImc instanceof ImcExpr sndExpr)) {
            throw new Report.InternalError();
        }

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

    @Override
    public ImcInstr visit(AstCallExpr callExpr, Object arg) {
        var args = new LinkedList<ImcExpr>();
        callExpr.args.forEach(argExpr -> args.add((ImcExpr) argExpr.accept(this, arg)));

        // Get memory label
        var fnDefn = (AstFunDefn) SemAn.definedAt.get(callExpr);
        var frame = Memory.frames.get(fnDefn);
        var label = frame.label;

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
    public ImcInstr visit(AstCastExpr castExpr, Object arg) {
        castExpr.type.accept(this, arg);
        var exprImc = castExpr.expr.accept(this, arg);

        if (SemAn.isType.get(castExpr.type) instanceof SemCharType) {
            // Mod 256
            // todo
        }
        return null;
    }

    @Override
    public ImcInstr visit(AstCmpExpr cmpExpr, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstNameExpr nameExpr, Object arg) {
        var varDefn = (AstVarDefn) SemAn.definedAt.get(nameExpr);
        var access = Memory.varAccesses.get(varDefn);

        var imc = switch (access) {
            case MemAbsAccess absAccess -> {
                var label = absAccess.label;
                yield new ImcNAME(label);
            }
            case MemRelAccess relAccess -> {
                var fnDefn = (AstFunDefn) varDefn.parent;  // todo test hard!
                var frame = Memory.frames.get(fnDefn);

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
    public ImcInstr visit(AstPfxExpr pfxExpr, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstSfxExpr sfxExpr, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstSizeofExpr sizeofExpr, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstAssignStmt assignStmt, Object arg) {
        var dstImc = assignStmt.dst.accept(this, arg);
        var srcImc = assignStmt.src.accept(this, arg);

        if (!(dstImc instanceof ImcExpr dstExpr) || !(srcImc instanceof ImcExpr srcExpr)) {
            throw new Report.InternalError();
        }

        var assignImc = new ImcMOVE(dstExpr, srcExpr);
        ImcGen.stmtImc.put(assignStmt, assignImc);

        return assignImc;
    }

    @Override
    public ImcInstr visit(AstBlockStmt blockStmt, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstExprStmt exprStmt, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstIfStmt ifStmt, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstReturnStmt retStmt, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstWhileStmt whileStmt, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstArrType arrType, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstAtomType atomType, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstNameType nameType, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstPtrType ptrType, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstStrType strType, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstUniType uniType, Object arg) {
        return null;
    }

    @Override
    public ImcInstr visit(AstRecType.AstCmpDefn cmpDefn, Object arg) {
        return null;
    }
}
