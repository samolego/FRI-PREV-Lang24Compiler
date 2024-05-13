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
        boolean changed;
        do {
            changed = false;
            for (int i = this.instrs.size() - 1; i >= 0; --i) {
                changed |= analyze(i);
            }
        } while (changed);
    }


    /**
     * Analyze the liveness of the instruction at the given index.
     * Assumes {@code index + 1} has been already analyzed.
     *
     * @param index Index of the instruction to analyze.
     * @return True if the in or out sets have changed (another iteration is needed).
     */
    private boolean analyze(int index) {
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
        int oldSize = instruction.out().size();
        instruction.addOutTemp(sucIns);
        boolean changedOuts = oldSize != instruction.out().size();

        final var in = new HashSet<>(instruction.out());
        instruction.defs().forEach(in::remove);
        in.addAll(instruction.uses());

        oldSize = instruction.in().size();
        instruction.addInTemps(in);
        boolean changedIns = oldSize != instruction.in().size();

        return changedOuts || changedIns;
    }

    private Set<MemTemp> getIns(int index) {
        var instr = this.instrs.get(index);

        if (!instr.in().isEmpty()) {
            return instr.in();
        }

        var outs = instr.out().isEmpty() ? this.getOuts(index) : instr.out();


        final var in = new HashSet<>(outs);
        instr.defs().forEach(in::remove);
        in.addAll(instr.uses());

        instr.addInTemps(in);

        return in;
    }

    private Set<MemTemp> getOuts(int index) {
        var instruction = this.instrs.get(index);
        Set<MemTemp> sucIns;

        if (instruction.jumps().isEmpty()) {
            // Only successor is next instruction
            sucIns = index == this.instrs.size() - 1
                    ? Collections.emptySet()  // out of bounds
                    : this.getIns(index + 1);  // We assume that the next instruction has been already analyzed
        } else {
            // Possibly multiple successors
            sucIns = new HashSet<>();
            Vector<AsmInstr> asmInstrs = this.instrs;
            for (int i = asmInstrs.size() - 1; i >= 0; --i) {
                AsmInstr instr = asmInstrs.get(i);
                if (instr instanceof AsmLABEL lbl) {
                    for (var jump : instruction.jumps()) {
                        if (lbl.label == jump) {
                            var ins = this.getIns(i);
                            sucIns.addAll(ins);
                        }
                    }
                }
            }
        }

        instruction.addOutTemp(sucIns);

        return sucIns;
    }
}
