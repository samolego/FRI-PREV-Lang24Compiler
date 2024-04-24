package lang24.phase.imclin;

import lang24.common.report.Report;
import lang24.data.imc.code.ImcInstr;
import lang24.data.imc.code.expr.ImcBINOP;
import lang24.data.imc.code.expr.ImcCALL;
import lang24.data.imc.code.expr.ImcCONST;
import lang24.data.imc.code.expr.ImcExpr;
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
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.imc.visitor.AbstractImcVisitor;
import lang24.data.mem.MemTemp;

import java.util.LinkedList;
import java.util.List;

/**
 * Linearizes the tree of IMC statements into a list of IMC statements.
 */
public class CodeLinearizator implements AbstractImcVisitor<ImcExpr, List<ImcStmt>> {

    /**
     * Creates a move statement that moves the given expression to a new temporary variable.
     * @param expr The expression to move
     * @return {@link TempMove} with the temporary variable and the move statement
     */
    private static TempMove createMove(ImcExpr expr) {
        var temp = new ImcTEMP(new MemTemp());
        return new TempMove(temp, new ImcMOVE(temp, expr));
    }

    @Override
    public ImcExpr visit(ImcBINOP binOp, List<ImcStmt> linStmts) {

        // Check first expression for any changes
        var newFirst = binOp.fstExpr.accept(this, linStmts);

        // Check second expression for any changes
        var newSecond = binOp.sndExpr.accept(this, linStmts);


        if (newFirst != binOp.fstExpr || newSecond != binOp.sndExpr) {
            // Create new binop with changed args
            return new ImcBINOP(binOp.oper, newFirst, newSecond);
        }

        // Return old value, as it has not changed
        return binOp;
    }

    /**
     * Unpacks the arguments of the call and adds them to the list of linearized statements.
     *
     * @param call     The call to unpack
     * @param linStmts The list of linearized statements
     * @return {@code null}
     */
    @Override
    public ImcExpr visit(ImcCALL call, List<ImcStmt> linStmts) {
        // Unpack the arguments
        var newArgs = new LinkedList<ImcExpr>();
        for (var arg : call.args) {
            var newArg = arg.accept(this, linStmts);

            var temp = newArg;
            if (newArg != arg || containsMem(newArg)) {
                var moveToTemp = createMove(newArg);
                temp = moveToTemp.tempVar();

                linStmts.add(moveToTemp.moveStmt());
            }

            newArgs.add(temp);
        }

        // Create new ImcCALL
        var newCall = new ImcCALL(call.label, call.offs, newArgs);

        // Move the result to a temporary variable
        var moveToTemp = createMove(newCall);
        linStmts.add(moveToTemp.moveStmt());

        return moveToTemp.tempVar();
    }

    @Override
    public ImcExpr visit(ImcCJUMP cjump, List<ImcStmt> linStmts) {
        var newCond = cjump.cond.accept(this, linStmts);

        var newCJump = cjump;
        if (newCond != cjump.cond) {
            newCJump = new ImcCJUMP(newCond, cjump.posLabel, cjump.negLabel);
        }

        linStmts.add(newCJump);

        return null;
    }

    @Override
    public ImcExpr visit(ImcCONST constant, List<ImcStmt> linStmts) {
        return constant;
    }

    // Pomoje ni treba dodajat ker edina stvar, ki jo lahko spremeni estmt je call, ki pa pade ven v MOVE
    @Override
    public ImcExpr visit(ImcESTMT eStmt, List<ImcStmt> linStmts) {
        var newExpr = eStmt.expr.accept(this, linStmts);

        var newEStmt = eStmt;
        if (newExpr != eStmt.expr) {
            newEStmt = new ImcESTMT(newExpr);
        }

        linStmts.add(newEStmt);

        return null;
    }

    @Override
    public ImcExpr visit(ImcJUMP jump, List<ImcStmt> linStmts) {
        linStmts.add(jump);

        return null;
    }

    @Override
    public ImcExpr visit(ImcLABEL label, List<ImcStmt> linStmts) {
        linStmts.add(label);

        return null;
    }

    @Override
    public ImcExpr visit(ImcMEM mem, List<ImcStmt> linStmts) {
        mem.addr.accept(this, linStmts);
        return mem;
    }

    @Override
    public ImcExpr visit(ImcMOVE move, List<ImcStmt> linStmts) {
        var newDst = move.dst.accept(this, linStmts);
        var newSrc = move.src.accept(this, linStmts);

        var newMove = move;
        if (newDst != move.dst || newSrc != move.src) {
            newMove = new ImcMOVE(newDst, newSrc);
        }

        linStmts.add(newMove);

        return null;
    }

    @Override
    public ImcExpr visit(ImcNAME name, List<ImcStmt> linStmts) {
        return name;
    }

    @Override
    public ImcExpr visit(ImcSEXPR sExpr, List<ImcStmt> linStmts) {
        sExpr.stmt.accept(this, linStmts);
        var newExpr = sExpr.expr.accept(this, linStmts);

        if (newExpr != sExpr.expr) {
            return new ImcSEXPR(sExpr.stmt, newExpr);
        }

        // Stayed the same, no need to change it
        return sExpr;
    }

    @Override
    public ImcExpr visit(ImcSTMTS stmts, List<ImcStmt> linStmts) {
        // Visit each statement
        for (var stmt : stmts.stmts) {
            stmt.accept(this, linStmts);
        }

        return null;
    }

    @Override
    public ImcExpr visit(ImcTEMP temp, List<ImcStmt> linStmts) {
        return temp;
    }

    @Override
    public ImcExpr visit(ImcUNOP unOp, List<ImcStmt> linStmts) {
        var newSubExpr = unOp.subExpr.accept(this, linStmts);

        if (newSubExpr != unOp.subExpr) {
            return new ImcUNOP(unOp.oper, newSubExpr);
        }

        // Stayed the same, no need to change it
        return unOp;
    }


    private boolean containsMem(ImcInstr expr) {
        return switch (expr) {
            case ImcMEM ignored -> true;
            case ImcBINOP binOp -> containsMem(binOp.fstExpr) || containsMem(binOp.sndExpr);
            case ImcCALL call -> call.args.stream().anyMatch(this::containsMem);
            case ImcCONST ignored -> false;
            case ImcSEXPR sExpr -> containsMem(sExpr.expr);
            case ImcUNOP unOp -> containsMem(unOp.subExpr);
            // Statements
            case ImcTEMP ignored -> false;
            case ImcCJUMP cjump -> containsMem(cjump.cond);
            case ImcESTMT imcESTMT -> containsMem(imcESTMT.expr);
            case ImcJUMP ignored -> false;
            case ImcLABEL ignored -> false;
            case ImcMOVE move -> containsMem(move.dst) || containsMem(move.src);
            case ImcSTMTS stmts -> stmts.stmts.stream().anyMatch(this::containsMem);
            case ImcNAME ignored -> false;

            default -> throw new Report.InternalError();
        };
    }
}

record TempMove(ImcTEMP tempVar, ImcMOVE moveStmt) { }
