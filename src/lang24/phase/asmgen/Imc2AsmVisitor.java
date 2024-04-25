package lang24.phase.asmgen;

import lang24.common.report.Report;
import lang24.data.asm.AsmInstr;
import lang24.data.asm.AsmLABEL;
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

import java.util.List;

public class Imc2AsmVisitor implements AbstractImcVisitor<List<AsmInstr>, Object> {
    @Override
    public List<AsmInstr> visit(ImcBINOP binOp, Object visArg) {
        return null;
    }

    @Override
    public List<AsmInstr> visit(ImcCALL call, Object visArg) {
        return null;
    }

    @Override
    public List<AsmInstr> visit(ImcCJUMP cjump, Object visArg) {
        return null;
    }

    @Override
    public List<AsmInstr> visit(ImcCONST constant, Object visArg) {
        return null;
    }

    @Override
    public List<AsmInstr> visit(ImcESTMT eStmt, Object visArg) {
        return null;
    }

    @Override
    public List<AsmInstr> visit(ImcJUMP jump, Object visArg) {
        return null;
    }

    @Override
    public List<AsmInstr> visit(ImcLABEL label, Object visArg) {
        return List.of(new AsmLABEL(label.label));
    }

    @Override
    public List<AsmInstr> visit(ImcMEM mem, Object visArg) {
        return null;
    }

    @Override
    public List<AsmInstr> visit(ImcMOVE move, Object visArg) {
        return null;
    }

    @Override
    public List<AsmInstr> visit(ImcNAME name, Object visArg) {
        return null;
    }

    @Override
    public List<AsmInstr> visit(ImcSEXPR sExpr, Object visArg) {
        // Shouldn't have sExpr at this point
        throw new Report.InternalError();
    }

    @Override
    public List<AsmInstr> visit(ImcSTMTS stmts, Object visArg) {
        // Shouldn't have stmts at this point
        throw new Report.InternalError();
    }

    @Override
    public List<AsmInstr> visit(ImcTEMP temp, Object visArg) {
        return null;
    }

    @Override
    public List<AsmInstr> visit(ImcUNOP unOp, Object visArg) {
        return null;
    }
}
