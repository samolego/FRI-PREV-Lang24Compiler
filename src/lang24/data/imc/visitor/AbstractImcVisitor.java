package lang24.data.imc.visitor;

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

public interface AbstractImcVisitor<Result, Arg> {

    Result visit(ImcBINOP binOp, Arg visArg);

    Result visit(ImcCALL call, Arg visArg);

    Result visit(ImcCJUMP cjump, Arg visArg);

    Result visit(ImcCONST constant, Arg visArg);

    Result visit(ImcESTMT eStmt, Arg visArg);

    Result visit(ImcJUMP jump, Arg visArg);

    Result visit(ImcLABEL label, Arg visArg);

    Result visit(ImcMEM mem, Arg visArg);

    Result visit(ImcMOVE move, Arg visArg);

    Result visit(ImcNAME name, Arg visArg);

    Result visit(ImcSEXPR sExpr, Arg visArg);

    Result visit(ImcSTMTS stmts, Arg visArg);

    Result visit(ImcTEMP temp, Arg visArg);

    Result visit(ImcUNOP unOp, Arg visArg);

}
