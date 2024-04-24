package lang24.phase.imclin;

import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.imc.code.stmt.ImcLABEL;
import lang24.data.imc.code.stmt.ImcStmt;
import lang24.data.lin.LinCodeChunk;
import lang24.data.lin.LinDataChunk;
import lang24.data.mem.MemAbsAccess;
import lang24.phase.imcgen.ImcGen;
import lang24.phase.memory.Memory;

import java.util.LinkedList;
import java.util.List;

public class ChunkGenerator implements AstFullVisitor<Void, List<ImcStmt>> {


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

        // Statement list for the body of the function
        var bodyStmts = new LinkedList<ImcStmt>();

        // Prologue
        var entryLabel = ImcGen.entryLabel.get(funDefn);
        bodyStmts.add(new ImcLABEL(entryLabel));


        // Get the body of the function & unpack ImcSTMTS to List<ImcStmt>
        var body = ImcGen.stmtImc.get(funDefn.stmt);
        body.accept(new CodeLinearizator(), bodyStmts);


        // Epilogue
        var exitLabel = ImcGen.exitLabel.get(funDefn);
        bodyStmts.add(new ImcLABEL(exitLabel));

        // Get function information
        var fnFrame = Memory.frames.get(funDefn);

        // Create function code chunk
        var codeChunk = new LinCodeChunk(fnFrame, bodyStmts, entryLabel, exitLabel);
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
}
