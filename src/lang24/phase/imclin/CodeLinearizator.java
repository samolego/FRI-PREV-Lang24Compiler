package lang24.phase.imclin;

import lang24.common.Pair;
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

    private static Pair<ImcTEMP, ImcMOVE> createMove(ImcExpr expr) {
        var temp = new ImcTEMP(new MemTemp());
        return new Pair<>(temp, new ImcMOVE(temp, expr));
    }

    @Override
    public ImcExpr visit(ImcBINOP binOp, List<ImcStmt> linStmts) {
        boolean changed = false;

        var first = binOp.fstExpr;
        var newFirst = first.accept(this, linStmts);
        if (newFirst != first) {
            var moved = createMove(newFirst);
            linStmts.add(moved.right());

            first = moved.left();
            changed = true;
        }

        var second = binOp.sndExpr;
        var newSecond = second.accept(this, linStmts);
        if (newSecond != second) {
            var moved = createMove(newSecond);
            linStmts.add(moved.right());

            second = moved.left();
            changed = true;
        }


        if (changed) {
            // Create new binop with changed
            return new ImcBINOP(binOp.oper, first, second);
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
            var argExpr = arg.accept(this, linStmts);

            var temp = argExpr;
            if (argExpr != arg) {
                temp = new ImcTEMP(new MemTemp());
                var moved = new ImcMOVE(temp, arg);

                linStmts.add(moved);
            }

            newArgs.add(temp);
        }

        // Create new ImcCALL
        return new ImcCALL(call.label, call.offs, newArgs);
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
        var newAddr = mem.addr.accept(this, linStmts);

        return new ImcMEM(newAddr);
    }

    @Override
    public ImcExpr visit(ImcMOVE move, List<ImcStmt> linStmts) {
        boolean changed = false;

        var newDst = move.dst.accept(this, linStmts);
        if (newDst != move.dst) {
            var moved = createMove(newDst);
            linStmts.add(moved.right());

            newDst = moved.left();
            changed = true;
        }

        var newSrc = move.src.accept(this, linStmts);
        if (newSrc != move.src) {
            var moved = createMove(newSrc);

            linStmts.add(moved.right());

            newSrc = moved.left();
            changed = true;
        }

        var newMove = move;
        if (changed) {
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

        if (newExpr == sExpr.expr) {
            // Stayed the same, no need to change it
            return sExpr;
        }

        return new ImcSEXPR(sExpr.stmt, newExpr);
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

        if (newSubExpr == unOp.subExpr) {
            // Stayed the same, no need to change it
            return unOp;
        }

        return new ImcUNOP(unOp.oper, newSubExpr);
    }
}
