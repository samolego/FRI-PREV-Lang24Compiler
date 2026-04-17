package lang24.data.wat;

import lang24.data.imc.code.expr.*;
import lang24.data.imc.code.stmt.*;
import lang24.data.imc.visitor.ImcFullVisitor;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemFrame;
import lang24.phase.memory.Memory;
import lang24.phase.seman.SemAn;
import lang24.data.type.SemVoidType;
import lang24.data.type.WatType;

import java.util.Map;

/**
 * Returns Boolean: true if the instruction left a value on the Wasm stack.
 */
public class WatVisitor implements ImcFullVisitor<Boolean, Void> {
    private final WatWriter out;
    private final Map<MemLabel, Long> dataLabels;
    private final Map<MemLabel, Integer> labelToIndex;

    public WatVisitor(WatWriter out, Map<MemLabel, Long> dataLabels, Map<MemLabel, Integer> labelToIndex) {
        this.out = out;
        this.dataLabels = dataLabels;
        this.labelToIndex = labelToIndex;
    }

    @Override
    public Boolean visit(ImcCONST constant, Void arg) {
        out.println("(i64.const %d)", constant.value);
        return true;
    }

    @Override
    public Boolean visit(ImcTEMP temp, Void arg) {
        out.println("(local.get $T%d)", temp.temp.temp);
        return true;
    }

    @Override
    public Boolean visit(ImcBINOP binOp, Void arg) {
        binOp.fstExpr.accept(this, arg);
        binOp.sndExpr.accept(this, arg);
        String op = switch (binOp.oper) {
            case ADD -> "i64.add";
            case SUB -> "i64.sub";
            case MUL -> "i64.mul";
            case DIV -> "i64.div_s";
            case MOD -> "i64.rem_s";
            case AND -> "i64.and";
            case OR  -> "i64.or";
            default  -> {
                String comp = switch (binOp.oper) {
                    case EQU -> "i64.eq";
                    case NEQ -> "i64.ne";
                    case LTH -> "i64.lt_s";
                    case GTH -> "i64.gt_s";
                    case LEQ -> "i64.le_s";
                    case GEQ -> "i64.ge_s";
                    default -> throw new IllegalStateException();
                };
                out.println(comp);
                yield "i64.extend_i32_u";
            }
        };
        out.println(op);
        return true;
    }

    @Override
    public Boolean visit(ImcUNOP unOp, Void arg) {
        unOp.subExpr.accept(this, arg);
        if (unOp.oper == ImcUNOP.Oper.NEG) {
            out.println("(i64.const -1)");
            out.println("i64.mul");
        } else if (unOp.oper == ImcUNOP.Oper.NOT) {
            out.println("i64.eqz");
            out.println("i64.extend_i32_u");
        }
        return true;
    }

    @Override
    public Boolean visit(ImcMEM mem, Void arg) {
        mem.addr.accept(this, arg);
        out.println("i32.wrap_i64");
        out.println("i64.load");
        return true;
    }

    @Override
    public Boolean visit(ImcNAME name, Void arg) {
        Long addr = dataLabels.get(name.label);
        out.println("(i64.const %d)", addr != null ? addr : 0);
        return true;
    }

    @Override
    public Boolean visit(ImcMOVE move, Void arg) {
        if (move.dst instanceof ImcTEMP temp) {
            // Run the source expression and see if it pushed anything
            if (move.src.accept(this, null)) {
                // Only if it pushed a value do we emit the set
                out.println("(local.set $T%d)", temp.temp.temp);
            }
        } else if (move.dst instanceof ImcMEM mem) {
            mem.addr.accept(this, null);
            out.println("i32.wrap_i64");
            if (move.src.accept(this, null)) {
                out.println("i64.store");
            } else {
                // If the source was void, we have an address on the stack
                // but no value to store. We must clear the address!
                out.println("drop");
            }
        }
        return false; // A move is a statement; it leaves nothing on the stack
    }


    @Override
    public Boolean visit(ImcCALL call, Void arg) {
        String name = call.label.name();
        boolean external = Memory.externalFns.containsKey(call.label);

        if (external) {
            for (int i = 1; i < call.args.size(); i++) {
                call.args.get(i).accept(this, arg);
            }
        } else {
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

        var resType = Memory.resultTypes.get(call.label);
        return !(resType instanceof SemVoidType);
    }

    @Override
    public Boolean visit(ImcJUMP jump, Void arg) {
        out.println("(local.set $target (i32.const %d))", labelToIndex.get(jump.label));
        out.println("br $L");
        return false;
    }

    @Override
    public Boolean visit(ImcCJUMP cjump, Void arg) {
        cjump.cond.accept(this, null); // Consume condition
        out.println("i32.wrap_i64");
        out.groupStart("(if");
        out.groupStart("(then");
        out.println("(local.set $target (i32.const %d))", labelToIndex.get(cjump.posLabel));
        out.println("br $L");
        out.groupEnd();
        out.groupStart("(else");
        out.println("(local.set $target (i32.const %d))", labelToIndex.get(cjump.negLabel));
        out.println("br $L");
        out.groupEnd();
        out.groupEnd();
        return false; // if consumes the stack value
    }

    @Override
    public Boolean visit(ImcESTMT eStmt, Void arg) {
        // Drop value only if it was produced
        if (eStmt.expr.accept(this, null)) {
            out.println("drop");
        }
        return false;
    }
}
