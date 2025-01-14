package lang24.data.asm;

import lang24.phase.regall.RegAll;

import java.util.LinkedList;
import java.util.List;

public class AsmLine {
    public static final String TAB = "\t";
    public static final AsmLine EMPTY = new AsmLine("");

    private final String str;

    private AsmLine(String str) {
        this.str = str;
    }

    public static AsmLine comment(String instr) {
        return new AsmLine(TAB + TAB + "// " + instr);
    }

    public static AsmLine instr(String instr) {
        return new AsmLine(TAB + TAB + instr);
    }

    public static AsmLine labeled(String label, String instruction) {
        return new AsmLine(label + TAB + instruction);
    }

    public static AsmLine of(AsmInstr instr) {
        return new AsmLine(TAB + TAB + instr.toString(RegAll.tempToReg));
    }

    public static List<AsmLine> of(List<AsmInstr> instrs) {
        var instructionLines = new LinkedList<AsmLine>();

        for (var instr : instrs) {
            if (instr instanceof AsmLABEL lbl) {
                // Label + noop (can be done better but blocks are not sorted, therefore two labels can appear one after another)
                instructionLines.add(new AsmLine(lbl + TAB + "ADDU $0,$0,0"));
            } else {
                instructionLines.add(of(instr));
            }
        }

        return instructionLines;
    }

    @Override
    public String toString() {
        return str;
    }
}
