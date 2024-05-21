package lang24.data.asm;

import lang24.phase.regall.RegAll;

import java.util.LinkedList;
import java.util.List;

public class AsmLine {
    public static final char TAB = '\t';
    private final String str;

    private AsmLine(String str) {
        this.str = str;
    }


    public static AsmLine comment(String instr) {
        return new AsmLine("// " + instr);
    }

    public static AsmLine instr(String instr) {
        return new AsmLine(TAB + instr);
    }

    public static AsmLine labeled(String label, String instruction) {
        return new AsmLine(label + TAB + instruction);
    }

    public static AsmLine of(AsmInstr instr) {
        return new AsmLine(TAB + instr.toString(RegAll.tempToReg));
    }

    public static List<AsmLine> of(List<AsmInstr> instrs) {
        var instructionLines = new LinkedList<AsmLine>();
        AsmLABEL label = null;

        for (var instr : instrs) {
            if (instr instanceof AsmLABEL lbl) {
                label = lbl;
            } else if (label != null) {
                instructionLines.add(new AsmLine(label.toString() + TAB + instr.toString(RegAll.tempToReg)));
                label = null;
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
