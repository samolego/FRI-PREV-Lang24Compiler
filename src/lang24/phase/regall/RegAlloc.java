package lang24.phase.regall;

import lang24.common.report.Report;
import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmOPER;
import lang24.data.asm.Code;
import lang24.data.mem.MemTemp;
import lang24.data.type.SemPointerType;
import lang24.phase.asmgen.Imc2AsmVisitor;
import lang24.phase.livean.LiveAnAlyser;
import lang24.phase.memory.MemEvaluator;

import java.util.Map;
import java.util.Vector;

import static lang24.phase.asmgen.Imc2AsmVisitor.Vector_of;

public class RegAlloc {
    public static int MAX_REGISTERS = 8;
    private final Code code;

    public RegAlloc(Code code) {
        this.code = code;
    }


    /**
     * Allocate registers for the code.
     * @param tempToReg Mapping of temporary variables to registers.
     */
    public void allocate(Map<MemTemp, Integer> tempToReg) {
        var graph = new Graph(this.code);

        var spilled = graph.colorAll(MAX_REGISTERS);

        if (!spilled.isEmpty()) {
            // Oh no, we have to spill some variables
            Report.info("Spilling variables: " + spilled);
            spilled.forEach(this::generateSpillCode);

            new LiveAnAlyser(code.instrs).analyzeAll();

            this.allocate(tempToReg);
            return;
        }

        tempToReg.putAll(graph.getColors());
    }


    /**
     * Oh my, hideous code for spilling variables.
     * @param memTemp Variable to spill.
     */
    private void generateSpillCode(MemTemp memTemp) {
        // automatic vars + old FP, return address + other temporaries
        long offset = this.code.frame.localSize + MemEvaluator.getSizeInBytes(SemPointerType.type) * 2 + this.code.tempCount * 8;
        var storeInstr = new AsmOPER("STOU `s0,%s,%d".formatted(Imc2AsmVisitor.FP, offset), Vector_of(memTemp), null, null);

        ++this.code.tempCount;

        boolean added = false;
        // Find the instruction that uses the spilled variable
        Vector<AsmInstr> instrs = this.code.instrs;
        int i = 0;
        while (i < instrs.size()) {
            var instr = instrs.get(i);

            if (instr.uses().contains(memTemp) && added) {
                // Load variable from memory
                var tmp = new MemTemp();
                var popInstr = new AsmOPER("LDOU `d0, %s,%d".formatted(Imc2AsmVisitor.FP, offset), null, Vector_of(tmp), null);
                instrs.insertElementAt(popInstr, i);

                // Replace instruction with new one
                var uses = instr.uses();
                int ix = uses.indexOf(memTemp);
                do {
                    uses.set(ix, tmp);
                    ix = uses.indexOf(memTemp);
                } while (ix != -1);

                var defs = instr.defs();
                ix = defs.indexOf(memTemp);
                boolean needsStore = false;
                while (ix != -1) {
                    defs.set(ix, tmp);
                    ix = defs.indexOf(memTemp);
                    needsStore = true;
                }

                var newInstr = new AsmOPER(((AsmOPER) instr).instr(), uses, defs, instr.jumps());
                instrs.set(i + 1, newInstr);

                if (needsStore) {
                    ++i;
                    var storeIns = new AsmOPER("STOU `s0,%s,%d".formatted(Imc2AsmVisitor.FP, offset), Vector_of(tmp), null, null);
                    instrs.add(i + 1, storeIns);
                }

                ++i;
            } else if (instr.defs().contains(memTemp)) {
                // Save variable to memory
                instrs.add(i + 1, storeInstr);
                added = true;
                ++i;
            }
            ++i;
        }
    }
}
