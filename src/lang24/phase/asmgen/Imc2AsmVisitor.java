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
import lang24.data.lin.LinCodeChunk;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemTemp;
import lang24.phase.regall.RegAlloc;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

/**
 * ImcCode to MMIX assembly code visitor.
 */
public class Imc2AsmVisitor implements ImcVisitor<MemTemp, List<AsmInstr>> {

    public static final String SP = "$254";
    public static final String FP = "$253";
    
    private final LinCodeChunk codeChunk;
    
    public Imc2AsmVisitor(LinCodeChunk codeChunk) {
        this.codeChunk = codeChunk;
    }

    @Override
    public MemTemp visit(ImcBINOP binOp, List<AsmInstr> instructions) {
        var fstDefs = binOp.fstExpr.accept(this, instructions);
        var snDefs = binOp.sndExpr.accept(this, instructions);

        var binopUses = Vector_of(fstDefs, snDefs);

        final var binopResult = new MemTemp();

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
                var divOp = genOper("DIV `d0,`s0,`s1", binopUses, divResultVec, null);
                instructions.add(divOp);

                // We don't need the source registers anymore
                binopUses = new Vector<>();

                // Add from the rR register to the result
                yield "GET `d0,rR";
            }
            default -> {
                // Comparison operators
                var newResTmp = new MemTemp();
                var newResultVec = Vector_of(newResTmp);

                // First execute the comparison
                var cmpOper = genOper("CMP `d0,`s0,`s1", binopUses, newResultVec, null);
                instructions.add(cmpOper);

                // New uses are the result of the comparison
                binopUses = newResultVec;

                // Execute another instruction to get the result
                yield switch (binOp.oper) {
                    case EQU -> "ZSZ";  // 1 if 0, otherwise 0
                    case NEQ -> "ZSNZ";  // 1 if not 0, otherwise 0
                    case LTH -> "ZSN";  // 1 if negative
                    case GTH -> "ZSP";  // 1 if positive
                    case LEQ -> "ZSNP";  // 1 if non-positive
                    case GEQ -> "ZSNN";  // 1 if non-negative
                    default -> throw new Report.InternalError();
                } + " `d0,`s0,1";
            }
        };

        var binOpOper = genOper(instr, binopUses, Vector_of(binopResult), null);
        instructions.add(binOpOper);


        return binopResult;
    }

    @Override
    public MemTemp visit(ImcCALL call, List<AsmInstr> instructions) {
        // todo

        // Push arguments to the stack
        for (int i = call.args.size() - 1; i >= 0; --i) {
            var argExpr = call.args.get(i);
            long offset = call.offs.get(i);
            var temps = argExpr.accept(this, instructions);

            // Push the argument to the stack
            var pushInstr = genOper("STOU `s0,%s,%d".formatted(SP, offset), Vector_of(temps), null, null);
            instructions.add(pushInstr);
        }

        // Perform the function call
        String instr = String.format("PUSHJ $%d,%s", RegAlloc.MAX_REGISTERS, call.label.name);
        var jumps = Vector_of(call.label);

        var callOper = genOper(instr, null, null, jumps);
        instructions.add(callOper);

        // todo - wrong
        var resultTemp = new MemTemp();

        // Todo - ask about this
        var loadResult = genOper("LDOU `d0," + SP + ",0", null,  Vector_of(resultTemp), null);
        instructions.add(loadResult);

        return resultTemp;
    }

    @Override
    public MemTemp visit(ImcCJUMP cjump, List<AsmInstr> instructions) {
        String instr = "BNZ `s0,%s";
        var jumps = Vector_of(cjump.posLabel, cjump.negLabel);

        // Evaluate the condition
        var uses = cjump.cond.accept(this, instructions);

        // Only jump to positive label if condition is true - intentional, as
        // we already sorted the code blocks in the previous phase
        var cjumpOper = genOper(String.format(instr, cjump.posLabel.name), Vector_of(uses), null, jumps);
        instructions.add(cjumpOper);

        return null;
    }

    @Override
    public MemTemp visit(ImcCONST constant, List<AsmInstr> instructions) {
        // Move the constant to a temporary register and return it
        var resultDefn = new MemTemp();
        setRegisterToConstantVal(instructions, resultDefn, constant.value);

        return resultDefn;
    }


    @Override
    public MemTemp visit(ImcJUMP jump, List<AsmInstr> instructions) {
        String instr = String.format("JMP %s", jump.label.name);
        var jumps = Vector_of(jump.label);

        var asmOper = genOper(instr, null, null, jumps);
        instructions.add(asmOper);

        return null;
    }

    @Override
    public MemTemp visit(ImcLABEL label, List<AsmInstr> instructions) {
        instructions.add(new AsmLABEL(label.label));
        return null;
    }

    @Override
    public MemTemp visit(ImcMEM mem, List<AsmInstr> instructions) {
        var addrDefs = mem.addr.accept(this, instructions);

        final var resultTemp = new MemTemp();

        var memInstr = genOper("LDOU `d0,`s0,0", Vector_of(addrDefs), Vector_of(resultTemp), null);
        instructions.add(memInstr);

        return resultTemp;
    }

    @Override
    public MemTemp visit(ImcMOVE move, List<AsmInstr> instructions) {
        if (move.dst instanceof ImcMEM mem) {
            return this.generateStoreInstruction(move, mem, instructions);
        }

        if (move.src instanceof ImcMEM mem) {
            return this.generateLoadInstruction(move, mem, instructions);
        }

        // Temp to temp move
        var dstDefs = move.dst.accept(this, instructions);
        var srcDefs = move.src.accept(this, instructions);


        var instruction = genAsmMove("ADD `d0,`s0,0", Vector_of(srcDefs), Vector_of(dstDefs));
        instructions.add(instruction);

        return dstDefs;
    }

    private MemTemp generateLoadInstruction(ImcMOVE move, ImcMEM mem, List<AsmInstr> instructions) {
        var addrDefs = mem.addr.accept(this, instructions);

        var destRegister = move.dst.accept(this, instructions);

        // Generate load
        var loadInstr = genOper("LDOU `d0,`s0,0", Vector_of(addrDefs), Vector_of(destRegister), null);
        instructions.add(loadInstr);

        return destRegister;
    }

    private MemTemp generateStoreInstruction(ImcMOVE move, ImcMEM mem, List<AsmInstr> instructions) {
        // Calculate address
        var addrDefs = mem.addr.accept(this, instructions);

        // Calculate value to store
        var valueDefs = move.src.accept(this, instructions);

        var uses = Vector_of(valueDefs, addrDefs);

        // Generate store
        var storeInstr = genOper("STOU `s0,`s1,0", uses, null, null);
        instructions.add(storeInstr);

        return null;
    }

    @Override
    public MemTemp visit(ImcNAME name, List<AsmInstr> instructions) {
        // Move name to temp and return it
        final var resultTemp = new MemTemp();
        final var instr = genOper(String.format("LDA `d0,%s", name.label.name), null,  Vector_of(resultTemp), null);
        instructions.add(instr);

        instructions.add(instr);

        return resultTemp;
    }

    @Override
    public MemTemp visit(ImcTEMP temp, List<AsmInstr> instructions) {
        return temp.temp;
    }

    @Override
    public MemTemp visit(ImcUNOP unOp, List<AsmInstr> instructions) {
        var subDef = unOp.subExpr.accept(this, instructions);
        var subDefs = subDef == null ? null : Vector_of(subDef);
        var resultTemp = new MemTemp();

        var instr = switch (unOp.oper) {
            case NEG -> "NEG `d0,`s0";
            case NOT -> {
                // XOR 0xFFFFFFFF_FFFFFFFF and the value
                var xorResult = new MemTemp();

                // Load 0xFFFFFFFF_FFFFFFFF in `d0
                setRegisterToConstantVal(instructions, xorResult, 0xFFFFFFFF_FFFFFFFFL);

                if (subDefs == null) {
                    subDefs = new Vector<>();
                }
                subDefs.add(xorResult);
                yield "XOR `d0,`s0,`s1";
            }
        };

        var unOpOper = genOper(instr, subDefs, Vector_of(resultTemp), null);
        instructions.add(unOpOper);

        return resultTemp;
    }

    // potential optimization: if value is negative, we can load the negated value (minus minus) and then negate it again
    private void setRegisterToConstantVal(List<AsmInstr> instructions, MemTemp destinationDefn, long value) {
        final var destDefnVec = Vector_of(destinationDefn);

        boolean negate = value < 0;
        if (negate) {
            value = -value;
        }

        // Add "AND with 0" instructions to set the register to the value 0
        boolean needsAnd = value == 0;
        // All four bytes of value must differ from 0 in order for needsAnd to be false
        for (long val = value; val != 0L; val >>>= 16) {
            if ((val & 0xFFFFL) == 0) {
                needsAnd = true;
                break;
            }
        }

        if (needsAnd) {
            // First AND register with 0 to reset it
            // We do that so we can skip setting the register to 0
            // It helps us below to save on instructions
            var and0 = genOper("SETL `d0,#0", null, destDefnVec, null);
            instructions.add(and0);
        }


        var setInstrs = List.of("SETL", "INCML", "INCMH", "INCH");
        for (int i = 0; i < setInstrs.size(); i++) {
            var setInstr = setInstrs.get(i);
            long shifted = value >>> (i * 16);
            if (shifted == 0) {
                // Not needed, save on instructions
                // Register was cleared before, so we don't need to set this part
                continue;
            }
            long val = shifted & 0xFFFF;
            var set0xFF = genOper(String.format("%s `d0,#%04X", setInstr, val), null, destDefnVec, null);
            instructions.add(set0xFF);
        }

        if (negate) {
            instructions.add(genOper("NEG `d0,`s0", destDefnVec, destDefnVec, null));
        }
    }

    private AsmInstr genOper(String instr, Vector<MemTemp> uses, Vector<MemTemp> defs, Vector<MemLabel> jumps) {
        var newInstr = replaceTempsWithRegisters(instr, uses, defs);
        return new AsmOPER(newInstr, uses, defs, jumps);
    }
    
    
    private AsmMOVE genAsmMove(String instr, Vector<MemTemp> uses, Vector<MemTemp> defs) {
        var newInstr = replaceTempsWithRegisters(instr, uses, defs);
        return new AsmMOVE(newInstr, uses, defs);
    }


    /**
     * Replaces occurences of FP temporary with the actual register number.
     * E.g. if FP = MemTemp(x), then "`si" or "`dj" that correspond to MemTemp(x) will be replaced with {@link #FP}
     * @param instr Instruction to replace in
     * @param uses Vector of uses
     * @param defs Vector of definitions
     * @return Instruction with replaced values
     */
    private String replaceTempsWithRegisters(String instr, Vector<MemTemp> uses, Vector<MemTemp> defs) {
        // Holds the temps to remove
        var remove = new LinkedList<MemTemp>();

        // Holds the number of framepointer replacements
        int minus = 0;
        // Check if any temps is framepointer and replace it with the actual register
        for (int i = 0; uses != null && i < uses.size(); i++) {
            if (uses.get(i) == this.codeChunk.frame.FP) {
                // Replace "`si" with FP
                instr = instr.replace("`s" + i, FP);
                remove.add(uses.get(i));
                minus += 1;
            } else {
                // Replace "`si" with `s(i - minus)
                instr = instr.replace("`s" + i, "`s" + (i - minus));
            }
        }
        if (uses != null) {
            uses.removeAll(remove);
        }

        remove.clear();
        minus = 0;
        for (int i = 0; defs != null && i < defs.size(); i++) {
            if (defs.get(i) == this.codeChunk.frame.FP) {
                // Replace "`di" with FP
                instr = instr.replace("`d" + i, FP);
                remove.add(defs.get(i));
            } else {
                // Replace "`di" with `d(i - minus)
                instr = instr.replace("`d" + i, "`d" + (i - minus));
            }
        }
        if (defs != null) {
            defs.removeAll(remove);
        }


        return instr;
    }


    /**
     * Helper method to create a vector of elements
     * @param elements Elements to add to the vector
     * @return Vector of elements
     * @param <E> Type of elements
     */
    @SafeVarargs
    public static <E> Vector<E> Vector_of(E... elements) {
        var vector = new Vector<E>();
        Collections.addAll(vector, elements);
        return vector;
    }
}
