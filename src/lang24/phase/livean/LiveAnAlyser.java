package lang24.phase.livean;

import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmLABEL;
import lang24.data.mem.MemTemp;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

/**
 * Liveliness analyser for asm instructions.
 */
public class LiveAnAlyser {
    private final Vector<AsmInstr> instrs;

    public LiveAnAlyser(Vector<AsmInstr> instr) {
        this.instrs = instr;
    }

    public void analyzeAll() {
        // Backward analysis
        for (int i = this.instrs.size() - 1; i >= 0; --i) {
            this.analyze(i);
        }
    }


    /**
     * Analyze the liveness of the instruction at the given index.
     * Assumes {@code index + 1} has been already analyzed.
     * @param index Index of the instruction to analyze.
     */
    public void analyze(int index) {
        final var instruction = this.instrs.get(index);

        Set<MemTemp> sucIns;

        if (instruction.jumps().isEmpty()) {
            // Only successor is next instruction
            sucIns = index == this.instrs.size() - 1
                    ? Collections.emptySet()  // out of bounds
                    : this.instrs.get(index + 1).in();  // We assume that the next instruction has been already analyzed
        } else {
            // Possibly multiple successors
            sucIns = new HashSet<>();
            for (AsmInstr instr : this.instrs) {
                if (instr instanceof AsmLABEL lbl) {
                    for (var jump : instruction.jumps()) {
                        if (lbl.label == jump) {
                            sucIns.addAll(instr.in());
                        }
                    }
                }
            }
        }

        // Fill in and out sets
        // in (n) := use(n) U [out(n) - def(n)]
        // out(n) := U in(succ)
        instruction.addOutTemp(sucIns);

        final var in = new HashSet<>(instruction.out());
        instruction.defs().forEach(in::remove);
        in.addAll(instruction.uses());

        instruction.addInTemps(in);
    }
}
