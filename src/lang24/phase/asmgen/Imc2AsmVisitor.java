package lang24.phase.asmgen;

import lang24.common.report.Report;
import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmLABEL;
import lang24.data.asm.AsmMOVE;
import lang24.data.asm.AsmOPER;
import lang24.data.imc.code.expr.ImcBINOP;
import lang24.data.imc.code.expr.ImcCALL;
import lang24.data.imc.code.expr.ImcCONST;
import lang24.data.imc.code.expr.ImcMEM;
import lang24.data.imc.code.expr.ImcNAME;
import lang24.data.imc.code.expr.ImcTEMP;
import lang24.data.imc.code.expr.ImcUNOP;
import lang24.data.imc.code.stmt.ImcCJUMP;
import lang24.data.imc.code.stmt.ImcJUMP;
import lang24.data.imc.code.stmt.ImcLABEL;
import lang24.data.imc.code.stmt.ImcMOVE;
import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.mem.MemTemp;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

/**
 * ImcCode to MMIX assembly code visitor.
 */
public class Imc2AsmVisitor implements ImcVisitor<Vector<MemTemp>, List<AsmInstr>> {

    private static final String SP = "$254";

    @Override
    public Vector<MemTemp> visit(ImcBINOP binOp, List<AsmInstr> instructions) {
        var fstDefs = binOp.fstExpr.accept(this, instructions);
        var snDefs = binOp.sndExpr.accept(this, instructions);

        var binopUses = new Vector<MemTemp>();
        binopUses.addAll(fstDefs);
        binopUses.addAll(snDefs);

        var binopResult = new MemTemp();
        var binopDefs = Vector_of(binopResult);

        // Get the right instruction
        var instr = switch (binOp.oper) {
            case OR -> "OR `d0,`s0,`s1";
            case AND -> "AND `d0,`s0,`s1";
            case ADD -> "ADD `d0,`s0,`s1";
            case SUB -> "SUB `d0,`s0,`s1";
            case MUL -> "MUL `d0,`s0,`s1";
            case DIV -> "DIV `d0,`s0,`s1";
            case MOD -> {
                // Modulo is a bit more complex
                var divResTmp = new MemTemp();
                var divResultVec = Vector_of(divResTmp);

                // First do the division
                var divOp = new AsmOPER("DIV `d0,`s0,`s1", binopUses, divResultVec, null);
                instructions.add(divOp);

                // We don't need the source registers anymore
                binopUses.clear();

                // Add from the rR register to the result
                yield "ADD `d0,$rR,0";
            }
            default -> {
                // Comparison operators
                var newResTmp = new MemTemp();
                var newResultVec = Vector_of(newResTmp);

                // First execute the comparison
                var cmpOper = new AsmOPER("CMP `d0,`s0,`s1", binopUses, newResultVec, null);
                instructions.add(cmpOper);

                // New uses are the result of the comparison
                binopUses = newResultVec;

                // Execute another instruction to get the result
                yield switch (binOp.oper) {
                    case EQU -> "ZSZ `d0,`s0,1";  // 1 if 0, otherwise 0
                    case NEQ -> "ZSNZ `d0,`s0,1";  // 1 if not 0, otherwise 0
                    case LTH -> "ZSN `d0,`s0,1";  // 1 if negative
                    case GTH -> "ZSP `d0,`s0,1";  // 1 if positive
                    case LEQ -> "ZSNP `d0,`s0,1";  // 1 if non-positive
                    case GEQ -> "ZSNN `d0,`s0,1";  // 1 if non-negative
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

        // Push arguments to the stack
        for (int i = call.args.size() - 1; i >= 0; --i) {
            var argExpr = call.args.get(i);
            long offset = call.offs.get(i);
            var temps = argExpr.accept(this, instructions);

            // Push the argument to the stack
            var pushInstr = new AsmOPER("STO `s0,%s,%d".formatted(SP, offset), temps, null, null);
            instructions.add(pushInstr);
        }

        // Perform the function call
        String instr = String.format("PUSHJ $8,%s", call.label.name);
        var jumps = Vector_of(call.label);

        var callOper = new AsmOPER(instr, null, null, jumps);
        instructions.add(callOper);

        // todo - wrong
        var resultTemp = new MemTemp();
        var resultDefs = Vector_of(resultTemp);

        // Todo - ask about this
        var loadResult = new AsmOPER("LDO `d0," + SP + ",0", null, resultDefs, null);
        instructions.add(loadResult);

        return resultDefs;
    }

    @Override
    public Vector<MemTemp> visit(ImcCJUMP cjump, List<AsmInstr> instructions) {
        String instr = "BNZ `s0,%s";
        var jumps = Vector_of(cjump.posLabel, cjump.negLabel);

        // Evaluate the condition
        var uses = cjump.cond.accept(this, instructions);

        // Only jump to positive label if condition is true - intentional, as
        // we already sorted the code blocks in the previous phase
        var cjumpOper = new AsmOPER(String.format(instr, cjump.posLabel.name), uses, null, jumps);
        instructions.add(cjumpOper);

        return null;
    }

    @Override
    public Vector<MemTemp> visit(ImcCONST constant, List<AsmInstr> instructions) {
        // Move the constant to a temporary register and return it
        var resultDefn = new MemTemp();
        var defs = Vector_of(resultDefn);
        setRegisterToConstantVal(instructions, defs, constant.value);

        return defs;
    }


    @Override
    public Vector<MemTemp> visit(ImcJUMP jump, List<AsmInstr> instructions) {
        String instr = String.format("JMP %s", jump.label.name);
        var jumps = Vector_of(jump.label);

        var asmOper = new AsmOPER(instr, null, null, jumps);
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

        var memInstr = new AsmOPER("LDO `d0,`s0,0", addrDefs, defs, null);
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

        // Temp to temp move
        var dstDefs = move.dst.accept(this, instructions);
        var srcDefs = move.src.accept(this, instructions);


        var instruction = new AsmMOVE("ADD `d0,`s0,0", srcDefs, dstDefs);
        instructions.add(instruction);

        return dstDefs;
    }

    private Vector<MemTemp> generateLoadInstruction(ImcMOVE move, ImcMEM mem, List<AsmInstr> instructions) {
        var addrDefs = mem.addr.accept(this, instructions);

        var destRegister = move.dst.accept(this, instructions);

        // Generate load
        var loadInstr = new AsmMOVE("LDO `d0,`s0,0", addrDefs, destRegister);
        instructions.add(loadInstr);

        return destRegister;
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
        var storeInstr = new AsmOPER("STO `s0,`s1,0", uses, null, null);
        instructions.add(storeInstr);

        return null;
    }

    @Override
    public Vector<MemTemp> visit(ImcNAME name, List<AsmInstr> instructions) {
        // Move name to temp and return it
        // todo - check if this is correct

        var resultTemp = new MemTemp();
        var defs = Vector_of(resultTemp);

        var instr = new AsmOPER(String.format("LDO `d0,%s,0", name.label.name), null, defs, null);
        instructions.add(instr);

        instructions.add(instr);

        return defs;
    }

    @Override
    public Vector<MemTemp> visit(ImcTEMP temp, List<AsmInstr> instructions) {
        return Vector_of(temp.temp);
    }

    @Override
    public Vector<MemTemp> visit(ImcUNOP unOp, List<AsmInstr> instructions) {
        var subDefs = unOp.subExpr.accept(this, instructions);
        var resultTemp = new MemTemp();
        var defs = Vector_of(resultTemp);

        var instr = switch (unOp.oper) {
            case NEG -> "NEG `d0,`s0";
            case NOT -> {
                // XOR 0xFFFFFFFF_FFFFFFFF and the value
                var xorResult = new MemTemp();
                var xorDefs = Vector_of(xorResult);

                // Load 0xFFFFFFFF_FFFFFFFF in `d0
                setRegisterToConstantVal(instructions, xorDefs, 0xFFFFFFFF_FFFFFFFFL);

                if (subDefs == null) {
                    subDefs = new Vector<>();
                }
                subDefs.add(xorResult);
                yield "XOR `d0,`s0,`s1";
            }
        };

        var unOpOper = new AsmOPER(instr, subDefs, defs, null);
        instructions.add(unOpOper);

        return defs;
    }

    private void setRegisterToConstantVal(List<AsmInstr> instructions, Vector<MemTemp> xorDefs, long value) {
        var setInstrs = List.of("SETL", "SETML", "SETMH", "SETH");
        for (int i = 0; i < setInstrs.size(); i++) {
            var setInstr = setInstrs.get(i);
            long shifted = value >>> (i * 16);
            if (shifted == 0) {
                // Not needed, save on instructions
                continue;
            }
            long val = shifted & 0xFFFF;
            var set0xFF = new AsmOPER(String.format("%s `d0,#%04X", setInstr, val), null, xorDefs, null);
            instructions.add(set0xFF);
        }
    }


    /**
     * Helper method to create a vector of elements
     * @param elements Elements to add to the vector
     * @return Vector of elements
     * @param <E> Type of elements
     */
    @SafeVarargs
    private static <E> Vector<E> Vector_of(E... elements) {
        var vector = new Vector<E>();
        Collections.addAll(vector, elements);
        return vector;
    }
}
