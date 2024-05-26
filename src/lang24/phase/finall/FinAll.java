package lang24.phase.finall;

import lang24.common.report.Report;
import lang24.data.asm.AsmLine;
import lang24.data.asm.Code;
import lang24.phase.asmgen.AsmGen;
import lang24.phase.imclin.ImcLin;
import lang24.phase.regall.RegAll;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static lang24.phase.memory.MemEvaluator.POINTER_SIZE;
import static lang24.phase.regall.RegAlloc.MAX_REGISTERS;

/**
 * Writes assembly code to a file, adding prologue and epilogue
 * for each function and start "program" to set heap and stack pointers.
 * @param filename
 */
public record FinAll(String filename) {
    private static final long HEAP_POINTER = 0x4000000000000000L;
    private static final long STACK_POINTER = 0x6000000000000000L;

    public void genAsmFile() {
        var instructions = new LinkedList<>(genEntryCode());
        // Add stdlib
        instructions.addAll(StdLib.STD_LIB_INSTRS);

        for (var code : AsmGen.codes) {
            instructions.add(AsmLine.EMPTY);
            instructions.add(AsmLine.EMPTY);
            instructions.addAll(genPrologue(code));
            instructions.add(AsmLine.EMPTY);
            instructions.addAll(AsmLine.of(code.instrs));
            instructions.add(AsmLine.EMPTY);
            instructions.addAll(genEpilogue(code));
            instructions.add(AsmLine.EMPTY);
        }

        // Write to file
        try (var writer = new PrintWriter(filename)) {
            for (var instruction : instructions) {
                writer.println(instruction);
            }
        } catch (IOException e) {
            throw new Report.Error("Cannot write to file: " + filename + ": " + e.getMessage());
        }
    }

    public List<AsmLine> genEntryCode() {
        var instructions = new LinkedList<AsmLine>();

        // Set up pointers
        // Trap register
        //instructions.add(AsmLine.labeled("TR", "GREG 0"));
        // Stack pointer
        instructions.add(AsmLine.labeled("SP", "GREG 0"));
        // Frame pointer
        instructions.add(AsmLine.labeled("FP", "GREG 0"));
        // Heap pointer
        instructions.add(AsmLine.labeled("HP", "GREG 0"));

        // Data segment
        instructions.add(AsmLine.instr("LOC Data_Segment"));
        // todo - add data segment
        this.addGlobalData(instructions);


        // Text segment
        instructions.add(AsmLine.instr("LOC #100"));
        instructions.add(AsmLine.labeled("Main", "SETL SP,#0"));
        instructions.addAll(setRegisterToConstantVal(254, STACK_POINTER));

        instructions.add(AsmLine.instr("SET FP,SP"));

        instructions.addAll(setRegisterToConstantVal(252, HEAP_POINTER));

        // Jump to _main
        instructions.add(AsmLine.instr("PUSHJ $" + MAX_REGISTERS + ",_main"));

        // Load return value
        instructions.add(AsmLine.instr("LDO $0,SP,#0"));

        // Halt
        instructions.add(AsmLine.instr("TRAP 0,Halt,0"));

        return instructions;
    }

    private void addGlobalData(LinkedList<AsmLine> instructions) {
        var chunks = ImcLin.dataChunks();

        for (var chunk : chunks) {
            var label = chunk.label;
            if (chunk.init != null) {
                var value = chunk.init.chars()
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(","));
                instructions.add(AsmLine.labeled(label.name, "OCTA " + value + ",0"));  // Add null terminator
            } else {
                boolean isCustomSize = false;
                var size = switch ((int) chunk.size) {
                    case 1 -> "BYTE";
                    case 2 -> "WYDE";
                    case 4 -> "TETRA";
                    case 8 -> "OCTA";
                    default -> {
                        isCustomSize = true;
                        yield "BYTE";
                    }
                };
                instructions.add(AsmLine.labeled(label.name, size + " 0"));
                if (isCustomSize) {
                    instructions.add(AsmLine.instr("LOC @a#" + (chunk.size - 1)));
                }
            }
        }
    }


    public List<AsmLine> genPrologue(Code code) {
        // Save current frame pointer and program counter
        var instructions = new LinkedList<AsmLine>();
        instructions.add(AsmLine.comment("Prologue " + code.frame.label.name + ", locals: " + code.frame.localSize + ", args: " + code.frame.argsSize + ", temps: " + code.tempCount));

        // Save current SP
        instructions.add(AsmLine.comment("Save current SP"));
        instructions.add(AsmLine.labeled(code.frame.label.name, "SET $0,SP"));

        // Subtract current SP by size of locals + 2 * pointer size
        instructions.add(AsmLine.comment("Subtract current sp by size locals + 2 * pointer size"));
        long size = code.frame.localSize + 2 * POINTER_SIZE;
        instructions.add(AsmLine.instr("SUB SP,SP," + size));

        // Store old FP
        instructions.add(AsmLine.comment("Store old FP"));
        instructions.add(AsmLine.instr("STO FP,SP,#" + POINTER_SIZE));

        // Store return address
        instructions.add(AsmLine.comment("Store return address"));
        instructions.add(AsmLine.instr("GET FP,rJ"));  // get return address
        instructions.add(AsmLine.instr("STO FP,SP,#0"));  // store it

        // Set frame pointer to old sp
        instructions.add(AsmLine.comment("Set frame pointer to old SP"));
        instructions.add(AsmLine.instr("SET FP,$0"));

        // Set new sp
        instructions.add(AsmLine.comment("Set new SP"));
        long otherSize = code.tempCount * POINTER_SIZE + code.frame.argsSize;
        instructions.add(AsmLine.instr("SUB SP,SP," + otherSize));

        // Jump to function body
        instructions.add(AsmLine.comment("Jump to function body"));
        instructions.add(AsmLine.instr("JMP " + code.entryLabel));

        instructions.add(AsmLine.comment("End prologue"));

        return instructions;
    }


    public List<AsmLine> genEpilogue(Code code) {
        // Save current frame pointer and program counter
        var instructions = new LinkedList<AsmLine>();

        instructions.add(AsmLine.comment("Epilogue"));

        // Store return value
        instructions.add(AsmLine.comment("Store return value on stack"));
        var returnReg = RegAll.tempToReg.get(code.frame.RV);
        instructions.add(AsmLine.instr("STO $" + returnReg + ",FP,0"));

        // Add to SP in order to then restore old FP and return address
        instructions.add(AsmLine.comment("Add to SP in order to then restore old FP and return address"));
        long size = code.frame.argsSize + code.tempCount * POINTER_SIZE;
        instructions.add(AsmLine.instr("ADD SP,SP," + size));


        // Load return address
        instructions.add(AsmLine.comment("Load & restore return address"));
        instructions.add(AsmLine.instr("LDO $0,SP,#0"));
        // Restore it
        instructions.add(AsmLine.instr("PUT rJ,$0"));

        // Load old FP
        instructions.add(AsmLine.comment("Load old FP"));
        instructions.add(AsmLine.instr("LDO $0,SP,#8"));

        // Resore SP
        instructions.add(AsmLine.comment("Restore SP"));
        instructions.add(AsmLine.instr("SET SP,FP"));

        // Restore FP
        instructions.add(AsmLine.comment("Restore FP"));
        instructions.add(AsmLine.instr("SET FP,$0"));

        instructions.add(AsmLine.instr("POP 0,0"));

        instructions.add(AsmLine.comment("End epilogue"));

        return instructions;
    }

    private List<AsmLine> setRegisterToConstantVal(int register, long value) {
        final var instructions = new LinkedList<AsmLine>();
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
            var and0 = String.format("SETL $%d,#0", register);
            instructions.add(AsmLine.instr(and0));
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
            var set0xFF = String.format("%s $%d,#%04X", setInstr, register, val);
            instructions.add(AsmLine.instr(set0xFF));
        }

        if (negate) {
            instructions.add(AsmLine.instr(String.format("NEG $%d,$%d", register, register)));
        }

        return instructions;
    }
}
