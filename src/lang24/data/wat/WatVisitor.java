package lang24.data.wat;

import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.*;
import lang24.data.imc.visitor.ImcFullVisitor;
import lang24.data.mem.MemLabel;
import lang24.phase.memory.Memory;

import java.util.Map;

public class WatVisitor implements ImcFullVisitor<Void, Void> {
    private final WatWriter out;
    private final Map<MemLabel, Long> dataLabels;
    private final Map<MemLabel, Integer> labelToIndex;

    private boolean lastExprProducedValue = false;

    public WatVisitor(WatWriter out, Map<MemLabel, Long> dataLabels, Map<MemLabel, Integer> labelToIndex) {
        this.out = out;
        this.dataLabels = dataLabels;
        this.labelToIndex = labelToIndex;
    }

    @Override
    public Void visit(ImcCONST constant, Void arg) {
        out.println("(i64.const %d)", constant.value);
        this.lastExprProducedValue = true;
        return null;
    }

    @Override
    public Void visit(ImcTEMP temp, Void arg) {
        out.println("(local.get $T%d)", temp.temp.temp);
        this.lastExprProducedValue = true;
        return null;
    }

    @Override
    public Void visit(ImcBINOP binOp, Void arg) {
        binOp.fstExpr.accept(this, arg);
        binOp.sndExpr.accept(this, arg);

        switch (binOp.oper) {
            case ADD -> out.println("i64.add");
            case SUB -> out.println("i64.sub");
            case MUL -> out.println("i64.mul");
            case DIV -> out.println("i64.div_s");
            case MOD -> out.println("i64.rem_s");
            case AND -> out.println("i64.and");
            case OR  -> out.println("i64.or");
            default  -> {
                // Comparison operators return i32 in Wasm, must extend to i64
                String op = switch (binOp.oper) {
                    case EQU -> "i64.eq";
                    case NEQ -> "i64.ne";
                    case LTH -> "i64.lt_s";
                    case GTH -> "i64.gt_s";
                    case LEQ -> "i64.le_s";
                    case GEQ -> "i64.ge_s";
                    default -> throw new IllegalStateException();
                };
                out.println(op);
                out.println("i64.extend_i32_u");
            }
        }
        this.lastExprProducedValue = true;
        return null;
    }

    @Override
    public Void visit(ImcUNOP unOp, Void arg) {
        unOp.subExpr.accept(this, arg);
        if (unOp.oper == ImcUNOP.Oper.NEG) {
            out.println("(i64.const -1)");
            out.println("i64.mul");
        } else if (unOp.oper == ImcUNOP.Oper.NOT) {
            out.println("i64.eqz"); // Returns i32
            out.println("i64.extend_i32_u");
        }
        this.lastExprProducedValue = true;
        return null;
    }

    @Override
    public Void visit(ImcMEM mem, Void arg) {
        mem.addr.accept(this, arg);
        out.println("i32.wrap_i64");
        out.println("i64.load");
        this.lastExprProducedValue = true;
        return null;
    }

    @Override
    public Void visit(ImcNAME name, Void arg) {
        Long addr = dataLabels.get(name.label);
        out.println("(i64.const %d)", addr != null ? addr : 0);
        this.lastExprProducedValue = true;
        return null;
    }

    @Override
    public Void visit(ImcMOVE move, Void arg) {
        if (move.dst instanceof ImcTEMP temp) {
            move.src.accept(this, null);
            out.println("(local.set $T%d)", temp.temp.temp);
        } else if (move.dst instanceof ImcMEM mem) {
            mem.addr.accept(this, null);
            out.println("i32.wrap_i64");
            move.src.accept(this, null);
            out.println("i64.store");
        }
        return null;
    }

    @Override
    public Void visit(ImcCALL call, Void arg) {
        String name = call.label.name();
        boolean definedFn = Memory.internalFns.contains(call.label);

        if (!definedFn) {
            // ABI: Wasm Stack (Skip Static Link)
            for (int i = 1; i < call.args.size(); i++) {
                call.args.get(i).accept(this, arg);
            }
        } else {
            // ABI: RAM Stack (Keep Static Link)
            for (int i = 0; i < call.args.size(); i++) {
                out.println("(global.get $SP)");
                out.println("(i64.const %d)", i * 8);
                out.println("i64.add");
                out.println("i32.wrap_i64");
                call.args.get(i).accept(this, arg);
                out.println("i64.store");
            }
        }

        out.println("(call $%s)", name);
        this.lastExprProducedValue = true;
        return null;

    }

    @Override
    public Void visit(ImcJUMP jump, Void arg) {
        out.println("(local.set $target (i32.const %d))", labelToIndex.get(jump.label));
        out.println("br $L");
        return null;
    }

    @Override
    public Void visit(ImcCJUMP cjump, Void arg) {
        cjump.cond.accept(this, null);
        out.println("i32.wrap_i64");
        out.groupStart("(if");
        out.groupStart("(then");
        out.println("(local.set $target (i32.const %d))", labelToIndex.get(cjump.posLabel));
        out.println("br $L");
        out.groupEnd(); // end then

        out.groupStart("(else");
        out.println("(local.set $target (i32.const %d))", labelToIndex.get(cjump.negLabel));
        out.println("br $L");
        out.groupEnd(); // end else
        
        out.groupEnd(); // end if
        return null;
    }

    @Override
    public Void visit(ImcESTMT eStmt, Void arg) {
        eStmt.expr.accept(this, null);
        if (this.lastExprProducedValue) {
            out.println("drop");
        }
        return null;
    }
}
