package lang24.phase.asmgen;

import lang24.data.asm.AsmInstr;
import lang24.data.asm.Code;
import lang24.data.lin.LinCodeChunk;
import lang24.phase.Phase;
import lang24.phase.imclin.ImcLin;

import java.util.LinkedList;
import java.util.Vector;

/**
 * Machine code generator.
 */
public class AsmGen extends Phase {

    public static Vector<Code> codes = new Vector<>();


    public AsmGen() {
        super("asmgen");
    }

    public void genAsmCodes() {
        for (LinCodeChunk codeChunk : ImcLin.codeChunks()) {
            var asmInstrs = new LinkedList<AsmInstr>();
            final Imc2AsmVisitor imc2AsmVisitor = new Imc2AsmVisitor(codeChunk);
            codeChunk.stmts().forEach(stmt -> stmt.accept(imc2AsmVisitor, asmInstrs));
            Code code = new Code(codeChunk.frame, codeChunk.entryLabel, codeChunk.exitLabel, asmInstrs);
            codes.add(code);
        }
    }

    public void log() {
        if (logger == null)
            return;
        for (Code code : AsmGen.codes) {
            logger.begElement("code");
            logger.addAttribute("prologue", code.entryLabel.name);
            logger.addAttribute("body", code.entryLabel.name);
            logger.addAttribute("epilogue", code.exitLabel.name);
            logger.addAttribute("tempsize", Long.toString(code.tempCount));
            code.frame.log(logger);
            logger.begElement("instructions");
            for (AsmInstr instr : code.instrs) {
                logger.begElement("instruction");
                logger.addAttribute("code", instr.toString());
                logger.endElement();
            }
            logger.endElement();
            logger.endElement();
        }
    }
}
