package lang24.phase.imclin;

import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.tree.expr.AstCallExpr;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.imc.code.ImcInstr;
import lang24.data.imc.code.expr.ImcCALL;
import lang24.data.imc.code.expr.ImcExpr;
import lang24.data.imc.code.expr.ImcTEMP;
import lang24.data.imc.code.stmt.ImcLABEL;
import lang24.data.imc.code.stmt.ImcMOVE;
import lang24.data.imc.code.stmt.ImcSTMTS;
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.lin.LinCodeChunk;
import lang24.data.lin.LinDataChunk;
import lang24.data.mem.MemAbsAccess;
import lang24.data.mem.MemTemp;
import lang24.phase.imcgen.ImcGen;
import lang24.phase.memory.Memory;

import java.util.LinkedList;
import java.util.List;

public class ChunkGenerator implements AstFullVisitor<Void, List<ImcStmt>> {

    /**
     * Returns a list of statements from the provided statement.
     *
     * @param statement The statement to get the list of statements from.
     * @return A list of statements from a statement.
     */
    private List<ImcStmt> fillStatements(ImcStmt statement) {
        var statements = new LinkedList<ImcStmt>();
        fillStatements(statement, statements);
        return statements;
    }

    /**
     * Fills a list with statements from the provided statement.
     *
     * @param statement     The statement to get the statements from.
     * @param statementList The list to fill.
     */
    private void fillStatements(ImcStmt statement, List<ImcStmt> statementList) {
        if (statement instanceof ImcSTMTS stmts) {
            for (ImcStmt stmt : stmts.stmts) {
                fillStatements(stmt, statementList);
            }
        } else {
            statementList.add(statement);
        }
    }

    @Override
    public Void visit(AstVarDefn varDefn, List<ImcStmt> stmtList) {
        // Get memory location
        var memAcc = Memory.varAccesses.get(varDefn);
        if (memAcc instanceof MemAbsAccess absAcc) {
            var dataChunk = new LinDataChunk(absAcc);

            ImcLin.addDataChunk(dataChunk);
        }

        return null;
    }

    @Override
    public Void visit(AstFunDefn funDefn, List<ImcStmt> stmtList) {
        if (funDefn.stmt == null) {
            return null;
        }
        AstFullVisitor.super.visit(funDefn, stmtList);

        // Get the body of the function & unpack ImcSTMTS to List<ImcStmt>
        var body = ImcGen.stmtImc.get(funDefn.stmt);
        var stmts = fillStatements(body);

        // Get function information
        var frame = Memory.frames.get(funDefn);

        // Prologue
        var entryLabel = ImcGen.entryLabel.get(funDefn);

        // Save the old FP


        stmts.addFirst(new ImcLABEL(entryLabel));

        // Epilogue
        var exitLabel = ImcGen.exitLabel.get(funDefn);
        stmts.add(new ImcLABEL(exitLabel));
        // Return value is saved in MemEvaluator.java
        // Resore the old FP


        // Create function code chunk
        var codeChunk = new LinCodeChunk(frame, stmts, entryLabel, exitLabel);
        ImcLin.addCodeChunk(codeChunk);

        return null;
    }


    @Override
    public Void visit(AstAtomExpr atomExpr, List<ImcStmt> stmtList) {
        if (atomExpr.type == AstAtomExpr.Type.STR) {
            var acc = Memory.strings.get(atomExpr);
            var dataChunk = new LinDataChunk(acc);

            ImcLin.addDataChunk(dataChunk);
        }

        return null;
    }

    // Todo
    @Override
    public Void visit(AstCallExpr callExpr, List<ImcStmt> stmtList) {
        // Visit the arguments
        callExpr.args.accept(this, stmtList);

        // Get the call imc
        var callImc = (ImcCALL) ImcGen.exprImc.get(callExpr);

        var linearized = new LinkedList<ImcInstr>();

        // Linearize the call imc
        // Put each parameter in new temp, if it's named / call
        for (ImcExpr imcExpr : callImc.args) {
            // Create a new temp for the parameter
            var temp = new ImcTEMP(new MemTemp());
            var moved = new ImcMOVE(temp, imcExpr);
            linearized.add(moved);
        }

        linearized.add(callImc);

        // Replace

        return null;

    }

    /*
    // Todo
    @Override
    public Void visit(AstIfStmt ifStmt, List<ImcStmt> stmtList) {
        var imcStmt = ImcGen.stmtImc.get(ifStmt);
    }

    // Todo
    @Override
    public Void visit(AstReturnStmt retStmt, List<ImcStmt> stmtList) {
        return null;
    }

    // Todo
    @Override
    public Void visit(AstWhileStmt whileStmt, List<ImcStmt> stmtList) {
        return null;
    }*/
}
