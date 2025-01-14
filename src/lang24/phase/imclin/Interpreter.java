package lang24.phase.imclin;

import lang24.common.report.Report;
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
import lang24.data.imc.visitor.ImcVisitor;
import lang24.data.lin.LinCodeChunk;
import lang24.data.lin.LinDataChunk;
import lang24.data.mem.MemFrame;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemTemp;

import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.Vector;

/**
 * Interpreter - for testing purposes only.
 */
public class Interpreter {

    private final Scanner scanner = new Scanner(System.in);

    private final boolean debug = Boolean.parseBoolean(System.getenv().getOrDefault("INTP_DEBUG", "false"));

    private final Random random;

    private final Map<Long, Byte> memory;

    private Map<MemTemp, Long> temps;

    private final Map<MemLabel, Long> dataMemLabels;

    private final Map<MemLabel, Integer> jumpMemLabels;

    private final Map<MemLabel, LinCodeChunk> callMemLabels;

    private final MemTemp SP;

    private MemTemp FP;

    private MemTemp RV;

    private final MemTemp HP;

    public Interpreter(Vector<LinDataChunk> dataChunks, Vector<LinCodeChunk> codeChunks) {
        random = new Random();

        this.memory = new TreeMap<>();
        this.temps = new TreeMap<>();

        SP = new MemTemp();
        tempST(SP, 0x7FFFFFFFFFFFFFF8L);
        HP = new MemTemp();
        tempST(HP, 0x2000000000000000L);

        this.dataMemLabels = new TreeMap<MemLabel, Long>();
        for (LinDataChunk dataChunk : dataChunks) {
            if (debug) {
                System.out.printf("### %s @ %s\n", dataChunk.label.name, toHex(tempLD(HP, false)));
            }
            this.dataMemLabels.put(dataChunk.label, tempLD(HP, false));
            if (dataChunk.init != null) {
                // Save strings
                for (int c = 0; c < dataChunk.init.length(); c++)
                    memST(tempLD(HP, false) + 8L * c, (long) dataChunk.init.charAt(c), false);
                memST(tempLD(HP, false) + 8L * dataChunk.init.length(), 0L, false);
            }
            tempST(HP, tempLD(HP, false) + dataChunk.size, debug);
        }
        if (debug)
            System.out.print("###\n");

        this.jumpMemLabels = new TreeMap<>();
        this.callMemLabels = new TreeMap<>();
        for (LinCodeChunk codeChunk : codeChunks) {
            this.callMemLabels.put(codeChunk.frame.label, codeChunk);
            Vector<ImcStmt> stmts = codeChunk.stmts();
            for (int stmtOffset = 0; stmtOffset < stmts.size(); stmtOffset++) {
                if (stmts.get(stmtOffset) instanceof ImcLABEL)
                    jumpMemLabels.put(((ImcLABEL) stmts.get(stmtOffset)).label, stmtOffset);
            }
        }
    }

    private String toHex(long addr) {
        return String.format("0x%016X", addr);
    }

    private void memST(Long address, Long value) {
        memST(address, value, debug);
    }

    private void memST(Long address, Long value, boolean debug) {
        if (debug)
            System.out.printf("### [%s] <- %d(=%s)\n", toHex(address), value, toHex(value));
        for (int b = 0; b <= 7; b++) {
            long longval = value % 0x100;
            byte byteval = (byte) longval;
            memory.put(address + b, byteval);
            value = value >> 8;
        }
    }

    private Long memLD(Long address) {
        return memLD(address, debug);
    }

    private Long memLD(Long address, boolean debug) {
        long value = 0L;
        for (int b = 7; b >= 0; b--) {
            Byte byteval = memory.get(address + b);
            if (byteval == null) {
                byteval = (byte) (random.nextLong() / 0x100);
                //Report.warning(String.format("INTERPRETER: Uninitialized memory location: %s", toHex(address + b)));
                // throw new Report.Error("INTERPRETER: Uninitialized memory location " +
                // (address + b) + ".");
            }
            long longval = (long) byteval;
            value = (value * 0x100) + (longval < 0 ? longval + 0x100 : longval);
        }
        if (debug)
            System.out.printf("### %d <- [%s]\n", value, toHex(address));
        return value;
    }

    private void tempST(MemTemp temp, Long value) {
        tempST(temp, value, debug);
    }

    private void tempST(MemTemp temp, Long value, boolean debug) {
        temps.put(temp, value);
        if (debug) {
            if (temp == SP) {
                System.out.printf("### SP <- %s\n", toHex(value));
                return;
            }
            if (temp == FP) {
                System.out.printf("### FP <- %s\n", toHex(value));
                return;
            }
            if (temp == RV) {
                System.out.printf("### RV <- %s\n", toHex(value));
                return;
            }
            if (temp == HP) {
                System.out.printf("### HP <- %s\n", toHex(value));
                return;
            }
            System.out.printf("### T%d <- %d\n", temp.temp, value);
        }
    }

    private Long tempLD(MemTemp temp) {
        return tempLD(temp, debug);
    }

    private Long tempLD(MemTemp temp, boolean debug) {
        Long value = temps.get(temp);
        if (value == null) {
            value = random.nextLong();
            throw new Report.Error("Uninitialized temporary variable T" + temp.temp + ".");
        }
        if (debug) {
            if (temp == SP) {
                System.out.printf("### %s <- SP\n", toHex(value));
                return value;
            }
            if (temp == FP) {
                System.out.printf("### %s <- FP\n", toHex(value));
                return value;
            }
            if (temp == RV) {
                System.out.printf("### %s <- RV\n", toHex(value));
                return value;
            }
            if (temp == HP) {
                System.out.printf("### %s <- HP\n", toHex(value));
                return value;
            }
            System.out.printf("### %d <- T%d (%d = %s)\n", value, temp.temp, value, toHex(value));
            return value;
        }
        return value;
    }

    private class ExprInterpreter implements ImcVisitor<Long, Object> {

        @Override
        public Long visit(ImcBINOP imcBinop, Object arg) {
            Long fstExpr = imcBinop.fstExpr.accept(this, null);
            Long sndExpr = imcBinop.sndExpr.accept(this, null);
            switch (imcBinop.oper) {
                case OR:
                    return (fstExpr != 0) | (sndExpr != 0) ? 1L : 0L;
                case AND:
                    return (fstExpr != 0) & (sndExpr != 0) ? 1L : 0L;
                case EQU:
                    return (fstExpr == sndExpr) ? 1L : 0L;
                case NEQ:
                    return (fstExpr != sndExpr) ? 1L : 0L;
                case LEQ:
                    return (fstExpr <= sndExpr) ? 1L : 0L;
                case GEQ:
                    return (fstExpr >= sndExpr) ? 1L : 0L;
                case LTH:
                    return (fstExpr < sndExpr) ? 1L : 0L;
                case GTH:
                    return (fstExpr > sndExpr) ? 1L : 0L;
                case ADD:
                    return fstExpr + sndExpr;
                case SUB:
                    return fstExpr - sndExpr;
                case MUL:
                    return fstExpr * sndExpr;
                case DIV:
                    return fstExpr / sndExpr;
                case MOD:
                    return fstExpr % sndExpr;
            }
            throw new Report.InternalError();
        }

        @Override
        public Long visit(ImcCALL imcCall, Object arg) {
            throw new Report.InternalError();
        }

        @Override
        public Long visit(ImcCONST imcConst, Object arg) {
            return imcConst.value;
        }

        @Override
        public Long visit(ImcMEM imcMem, Object arg) {
            return memLD(imcMem.addr.accept(this, null));
        }

        @Override
        public Long visit(ImcNAME imcName, Object arg) {
            return dataMemLabels.get(imcName.label);
        }

        @Override
        public Long visit(ImcSEXPR imcSExpr, Object arg) {
            throw new Report.InternalError();
        }

        @Override
        public Long visit(ImcTEMP imcMemTemp, Object arg) {
            return tempLD(imcMemTemp.temp);
        }

        @Override
        public Long visit(ImcUNOP imcUnop, Object arg) {
            Long subExpr = imcUnop.subExpr.accept(this, null);
            switch (imcUnop.oper) {
                case NOT:
                    return (subExpr == 0) ? 1L : 0L;
                case NEG:
                    return -subExpr;
            }
            throw new Report.InternalError();
        }

    }

    private class StmtInterpreter implements ImcVisitor<MemLabel, Object> {

        @Override
        public MemLabel visit(ImcCJUMP imcCJump, Object arg) {
            if (debug)
                System.out.println(imcCJump);
            Long cond = imcCJump.cond.accept(new ExprInterpreter(), null);
            return (cond != 0) ? imcCJump.posLabel : imcCJump.negLabel;
        }

        @Override
        public MemLabel visit(ImcESTMT imcEStmt, Object arg) {
            if (debug)
                System.out.println(imcEStmt);
            if (imcEStmt.expr instanceof ImcCALL) {
                call((ImcCALL) imcEStmt.expr);
                return null;
            }
            imcEStmt.expr.accept(new ExprInterpreter(), null);
            return null;
        }

        @Override
        public MemLabel visit(ImcJUMP imcJump, Object arg) {
            if (debug)
                System.out.println(imcJump);
            return imcJump.label;
        }

        @Override
        public MemLabel visit(ImcLABEL imcMemLabel, Object arg) {
            if (debug)
                System.out.println(imcMemLabel);
            return null;
        }

        @Override
        public MemLabel visit(ImcMOVE imcMove, Object arg) {
            if (debug)
                System.out.println(imcMove);
            if (imcMove.dst instanceof ImcMEM) {
                Long dst = ((ImcMEM) (imcMove.dst)).addr.accept(new ExprInterpreter(), null);
                Long src;
                if (imcMove.src instanceof ImcCALL) {
                    call((ImcCALL) imcMove.src);
                    src = memLD(tempLD(SP));
                } else
                    src = imcMove.src.accept(new ExprInterpreter(), null);
                memST(dst, src);
                return null;
            }
            if (imcMove.dst instanceof ImcTEMP) {
                ImcTEMP dst = (ImcTEMP) (imcMove.dst);
                Long src;
                if (imcMove.src instanceof ImcCALL) {
                    call((ImcCALL) imcMove.src);
                    src = memLD(tempLD(SP));
                } else
                    src = imcMove.src.accept(new ExprInterpreter(), null);
                tempST(dst.temp, src);
                return null;
            }
            throw new Report.InternalError();
        }

        @Override
        public MemLabel visit(ImcSTMTS imcStmts, Object arg) {
            if (debug)
                System.out.println(imcStmts);
            throw new Report.InternalError();
        }

        private void call(ImcCALL imcCall) {
            long offset = 0L;
            for (ImcExpr callArg : imcCall.args) {
                Long callValue = callArg.accept(new ExprInterpreter(), null);
                memST(tempLD(SP) + offset, callValue);
                offset += 8;
            }
            switch (imcCall.label.name) {
                case "_new" -> {
                    Long size = memLD(tempLD(SP, false) + 8, false);
                    Long addr = tempLD(HP);
                    tempST(HP, addr + size);
                    memST(tempLD(SP), addr, false);
                    return;
                }
                case "_del" -> {
                    return;
                }
                case "_exit" -> System.exit(1);
                case "_putint" -> {
                    Long c = memLD(tempLD(SP, false) + 8, false);
                    System.out.printf("%d", c);
                    return;
                }
                case "_getint" -> {
                    Long l = scanner.nextLong();
                    memST(tempLD(SP), l, false);
                    return;
                }
                case "_putchar" -> {
                    // +8 to skip static link
                    Long c = memLD(tempLD(SP, false) + 8, false);
                    System.out.printf("%c", (char) ((long) c) % 0x100);
                    return;
                }
                case "_getchar" -> {
                    char c = '\n';
                    try {
                        c = (char) System.in.read();
                    } catch (Exception ignored) {
                    }
                    memST(tempLD(SP), (long) c, false);
                    return;
                }
            }
            funCall(imcCall.label);
        }

    }

    public void funCall(MemLabel entryMemLabel) {

        Map<MemTemp, Long> storedMemTemps;
        MemTemp storedFP = null;
        MemTemp storedRV = null;

        LinCodeChunk chunk = callMemLabels.get(entryMemLabel);
        MemFrame frame = chunk.frame;
        Vector<ImcStmt> stmts = chunk.stmts();
        int stmtOffset;

        /* PROLOGUE */
        {
            if (debug)
                System.out.printf("###\n### CALL: %s\n", entryMemLabel.name);

            // Store registers and FP.
            storedMemTemps = temps;
            temps = new TreeMap<>(temps);
            // Store RA.
            // Create a stack frame.
            FP = frame.FP;
            RV = frame.RV;
            tempST(frame.FP, tempLD(SP));
            tempST(SP, tempLD(SP) - frame.size);
            // Jump to the body.
            stmtOffset = jumpMemLabels.get(chunk.entryLabel);
        }

        /* BODY */
        {
            int pc = 0;
            MemLabel label = null;

            while (label != chunk.exitLabel) {
                if (debug) {
                    pc++;
                    System.out.printf("### %s (%s):\n", chunk.frame.label.name, toHex(pc));
                    if (pc == 1000000)
                        break;
                }

                if (label != null) {
                    Integer offset = jumpMemLabels.get(label);
                    if (offset == null) {
                        throw new Report.InternalError();
                    }
                    stmtOffset = offset;
                }

                label = stmts.get(stmtOffset).accept(new StmtInterpreter(), null);

                stmtOffset += 1;
            }
        }

        /* EPILOGUE */
        {
            // Store the result.
            memST(tempLD(frame.FP), tempLD(frame.RV));
            // Destroy a stack frame.
            tempST(SP, tempLD(SP) + frame.size);
            // Restore registers and FP.
            FP = storedFP;
            RV = storedRV;
            Long hp = tempLD(HP);
            temps = storedMemTemps;
            tempST(HP, hp);
            // Restore RA.
            // Return.

            if (debug)
                System.out.printf("### RETURN: %s\n###\n", entryMemLabel.name);
        }

    }

    public long run(String entryMemLabel) {
        for (MemLabel label : callMemLabels.keySet()) {
            if (label.name.equals(entryMemLabel)) {
                funCall(label);
                return memLD(tempLD(SP));
            }
        }
        throw new Report.InternalError();
    }

}
