package lang24.data.wat;

import lang24.data.imc.code.expr.ImcTEMP;
import lang24.data.imc.visitor.ImcFullVisitor;
import lang24.data.mem.MemTemp;

import java.util.HashSet;
import java.util.Set;

public class TempVisitor implements ImcFullVisitor<Void, Void> {
    public final Set<MemTemp> temps;

    public TempVisitor() {
        this.temps = new HashSet<>();
    }

    @Override
    public Void visit(ImcTEMP temp, Void arg) {
        this.temps.add(temp.temp);
        return null;
    }
}
