package lang24.phase.asmgen;

import lang24.common.report.Report;
import lang24.common.report.Report.InternalError;
import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmLABEL;
import lang24.data.asm.AsmOPER;
import lang24.data.imc.code.expr.ImcBINOP;
import lang24.data.imc.code.expr.ImcCALL;
import lang24.data.imc.code.expr.ImcCONST;
import lang24.data.imc.code.expr.ImcMEM;
import lang24.data.imc.code.expr.ImcNAME;
import lang24.data.imc.code.expr.ImcSEXPR;
import lang24.data.imc.code.expr.ImcTEMP;
import lang24.data.imc.code.expr.ImcUNOP;
import lang24.data.imc.code.stmt.ImcCJUMP;
import lang24.data.imc.code.stmt.ImcESTMT;
import lang24.data.imc.code.stmt.ImcJUMP;
import lang24.data.imc.code.stmt.ImcLABEL;
import lang24.data.imc.code.stmt.ImcMOVE;
import lang24.data.imc.code.stmt.ImcSTMTS;
import lang24.data.imc.visitor.AbstractImcVisitor;
import lang24.data.mem.MemTemp;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * ImcCode to MMIX assembly code visitor.
 */
public class Imc2AsmVisitor implements AbstractImcVisitor<Vector<MemTemp>, List<AsmInstr>> {
    @Override
    public Vector<MemTemp> visit(ImcBINOP binOp, List<AsmInstr> instructions) {
        var fstDefs = binOp.fstExpr.accept(this, instructions);
        var snDefs = binOp.sndExpr.accept(this, instructions);

        var binopUses = new Vector<MemTemp>();
        binopUses.addAll(fstDefs);
        binopUses.addAll(snDefs);

        var binopResult = new MemTemp();
        var binopDefs = Vector_of(binopResult);

        var instr = switch(binOp.oper) {
            case OR -> "OR d0, s0, s1";
            case AND -> "AND d0, s0, s1";
            case ADD -> "ADD d0, s0, s1";
            case SUB, NEQ -> "SUB d0, s0, s1";
            case MUL -> "MUL d0, s0, s1";
            case DIV -> "DIV d0, s0, s1";
            case MOD -> {
                var divResTmp = new MemTemp();
                var divResultVec = Vector_of(divResTmp);

                var divOp = new AsmOPER("DIV d0, s0, s1", binopUses, divResultVec, null);
                instructions.add(divOp);

                binopUses = divResultVec;

                yield "ADD d0, s0, 0";
            }
            default -> {
                var newResTmp = new MemTemp();
                var newResultVec = Vector_of(newResTmp);
                var cmpOper = new AsmOPER("CMP d0, s0, s1", binopUses, newResultVec, null);

                instructions.add(cmpOper);

                binopUses = newResultVec;

                yield switch (binOp.oper) {
                    case EQU -> "ZSZ d0, s0, 1";  // 1 if 0, otherwise to 0
                    case LTH -> "ZSN d0, s0, 1";  // 1 if negative
                    case GTH -> "ZSP d0, s0, 1";  // 1 if positive
                    case LEQ -> "ZSNP d0, s0, 1";  // 1 if non-positive
                    case GEQ -> "ZSNN d0, s0, 1";  // 1 if non-negative
                    default -> throw new Report.InternalError();
                };
            }
        };

        var binOpOper = new AsmOPER(instr, binopUses, binopDefs, null);
        instructions.add(binOpOper);


        return binopDefs;
    }

    @Override
    public Vector<MemTemp> visit(ImcCALL call, List<AsmInstr> instructions) {
        // todo
        call.args.forEach(argExpr -> argExpr.accept(this, instructions));

        // Perform the function call
        String instr = "PUSHJ %s";
        var jumps = Vector_of(call.label);

        var callOper = new AsmOPER(String.format(instr, call.label.name), null, null, jumps);
        instructions.add(callOper);


        return null;
    }

    @Override
    public Vector<MemTemp> visit(ImcCJUMP cjump, List<AsmInstr> instructions) {
        // Only jump to positive label if condition is true
        String instr = "BNZ s0, %s";
        var jumps = Vector_of(cjump.posLabel);

        // Evaluate the condition
        var uses = cjump.cond.accept(this, instructions);

        var cjumpOper = new AsmOPER(String.format(instr, cjump.posLabel.name), uses, null, jumps);
        instructions.add(cjumpOper);

        return null;
    }

    @Override
    public Vector<MemTemp> visit(ImcCONST constant, List<AsmInstr> instructions) {
        // Move the constant to a temporary variable and return the temporary variable
        var instr = "SET d0, %s";
        var resultDefn = new MemTemp();
        var defs = Vector_of(resultDefn);

        var moveOper = new AsmOPER(String.format(instr, constant.value), null, defs, null);
        instructions.add(moveOper);

        return defs;
    }

    @Override
    public Vector<MemTemp> visit(ImcESTMT eStmt, List<AsmInstr> instructions) {
        // Shouldn't have eStmt at this point
        throw new InternalError();
    }

    @Override
    public Vector<MemTemp> visit(ImcJUMP jump, List<AsmInstr> instructions) {
        String instr = "JMP %s";
        var jumps = Vector_of(jump.label);

        var asmOper = new AsmOPER(String.format(instr, jump.label.name), null, null, jumps);
        instructions.add(asmOper);

        return null;
    }

    @Override
    public Vector<MemTemp> visit(ImcLABEL label, List<AsmInstr> instructions) {
        instructions.add(new AsmLABEL(label.label));
        return null;
    }

    @Override
    public Vector<MemTemp> visit(ImcMEM mem, List<AsmInstr> instructions) {
        var addrDefs = mem.addr.accept(this, instructions);
        var resultTemp = new MemTemp();
        var defs = Vector_of(resultTemp);

        var memInstr = new AsmOPER("LDO d0, s0", addrDefs, defs, null);
        instructions.add(memInstr);

        return defs;
    }

    @Override
    public Vector<MemTemp> visit(ImcMOVE move, List<AsmInstr> instructions) {
        if (move.dst instanceof ImcMEM mem) {
            return this.generateStoreInstruction(move, mem, instructions);
        }

        if (move.src instanceof ImcMEM mem) {
            return this.generateLoadInstruction(move, mem, instructions);
        }
        // Temp to temp

        var dstDefs = move.dst.accept(this, instructions);
        var srcDefs = move.src.accept(this, instructions);


        // Todo - change to move
        var instruction = new AsmOPER("ADD d0, s0, 0", srcDefs, dstDefs, null);
        instructions.add(instruction);

        return dstDefs;
    }

    private Vector<MemTemp> generateLoadInstruction(ImcMOVE move, ImcMEM mem, List<AsmInstr> instructions) {
        var addrDefs = mem.addr.accept(this, instructions);

        var loadDest = new MemTemp();
        var loadVec = Vector_of(loadDest);

        // Generate load
        var loadInstr = new AsmOPER("LDO d0, s0, 0", addrDefs, loadVec, null);
        instructions.add(loadInstr);

        // Move to dst
        return move.dst.accept(this, instructions);
    }

    private Vector<MemTemp> generateStoreInstruction(ImcMOVE move, ImcMEM mem, List<AsmInstr> instructions) {
        // Calculate address
        var addrDefs = mem.addr.accept(this, instructions);

        // Calculate value to store
        var valueDefs = move.src.accept(this, instructions);

        var uses = new Vector<MemTemp>();
        uses.addAll(addrDefs);
        uses.addAll(valueDefs);

        // Generate store
        var storeInstr = new AsmOPER("STO s0, s1, 0", uses, null, null);
        instructions.add(storeInstr);

        return null;
    }

    @Override
    public Vector<MemTemp> visit(ImcNAME name, List<AsmInstr> instructions) {
        // Move name to temp and return it
        var instr = "NAME TODO";  // todo - fix this
        var resultDefn = new MemTemp();
        var defs = Vector_of(resultDefn);

        var moveOper = new AsmOPER(String.format(instr, name.label.name), null, defs, null);
        instructions.add(moveOper);

        return defs;
    }

    @Override
    public Vector<MemTemp> visit(ImcSEXPR sExpr, List<AsmInstr> instructions) {
        // Shouldn't have sExpr at this point
        throw new InternalError();
    }

    @Override
    public Vector<MemTemp> visit(ImcSTMTS stmts, List<AsmInstr> instructions) {
        // Shouldn't have stmts at this point
        throw new InternalError();
    }

    @Override
    public Vector<MemTemp> visit(ImcTEMP temp, List<AsmInstr> instructions) {
        return Vector_of(temp.temp);
    }

    @Override
    public Vector<MemTemp> visit(ImcUNOP unOp, List<AsmInstr> instructions) {
        var subDefs = unOp.subExpr.accept(this, instructions);
        var resultTemp = new MemTemp();
        var defs = new Vector<MemTemp>();
        defs.add(resultTemp);

        var instr = switch (unOp.oper) {
            case NEG -> "NEG d0,s0";
            case NOT -> {
                // XOR 0xFFFFFFFF_FFFFFFFF and the value
                var xorDefs = new Vector<MemTemp>();
                var xorResult = new MemTemp();
                xorDefs.add(xorResult);

                // Todo - fix

                // s1 <= 0xFFFFFFFF_FFFFFFFF
                var set0xFF = new AsmOPER("NOT TODO //SET d0, 0xFFFFFFFF_FFFFFFFF", null, xorDefs, null);

                instructions.add(set0xFF);

                subDefs.add(xorResult);
                yield "XOR d0, s0, s1";
            }
        };

        var unOpOper = new AsmOPER(instr, subDefs, defs, null);
        instructions.add(unOpOper);

        return defs;
    }


    @SafeVarargs
    private static <E> Vector<E> Vector_of(E... elements) {
        var vector = new Vector<E>();
        Collections.addAll(vector, elements);
        return vector;
    }
}
