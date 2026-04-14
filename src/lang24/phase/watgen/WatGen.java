package lang24.phase.watgen;

import lang24.common.report.Report;
import lang24.data.imc.code.stmt.*;
import lang24.data.lin.LinCodeChunk;
import lang24.data.lin.LinDataChunk;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemTemp;
import lang24.data.mem.MemFrame;
import lang24.data.type.SemType;
import lang24.data.type.SemVoidType;
import lang24.data.wat.TempVisitor;
import lang24.data.wat.WatVisitor;
import lang24.data.wat.WatWriter;
import lang24.phase.Phase;
import lang24.phase.imclin.ImcLin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class WatGen extends Phase {

    private final String filename;
    private final WatWriter writer;

    private static final long HEAP_START = 0x10000;
    private static final long STACK_START = 0x800000;

    public WatGen(String filename) {
        super("watgen");
        this.filename = filename;
        try {
            // Note: WatWriter should wrap the PrintWriter
            this.writer = new WatWriter(new PrintWriter(filename));
        } catch (IOException e) {
            throw new Report.Error("Cannot write to file: " + filename + ": " + e.getMessage());
        }
    }

    @Override
    public void close() {
        writer.close();
        super.close();
    }

    public void genWatFile() {
        writer.groupStart("(module");

        // Imports
        writer.println("(import \"env\" \"putchar\" (func $putchar (param i32)))");
        writer.println("(import \"env\" \"getchar\" (func $getchar (result i32)))");
        writer.println("(import \"env\" \"exit\" (func $exit (param i32)))");

        writer.println("(memory (export \"memory\") 200)");

        writer.println("(global $SP (mut i64) (i64.const %d))", STACK_START);
        writer.println("(global $HP (mut i64) (i64.const %d))", HEAP_START);

        // Data chunks
        long currentDataOffset = 1024;
        Map<MemLabel, Long> dataLabels = new HashMap<>();
        for (LinDataChunk dataChunk : ImcLin.dataChunks()) {
            dataLabels.put(dataChunk.label, currentDataOffset);
            if (dataChunk.init != null) {
                // We must space characters out to 8 bytes each to match
                // the 8-byte size defined in MemEvaluator.java
                for (int i = 0; i < dataChunk.init.length(); i++) {
                    char c = dataChunk.init.charAt(i);
                    // Store char (1 byte) + 7 bytes of padding
                    writer.println("(data (i32.const %d) \"\\%02x\\00\\00\\00\\00\\00\\00\\00\")",
                            currentDataOffset + (i * 8L), (int) c);
                }
                // Null terminator (8 bytes of zeros)
                writer.println("(data (i32.const %d) \"\\00\\00\\00\\00\\00\\00\\00\\00\")",
                        currentDataOffset + (dataChunk.init.length() * 8L));

            }
            currentDataOffset += dataChunk.size;
            currentDataOffset = (currentDataOffset + 7) & ~7;
        }

        genStdLib();

        for (LinCodeChunk codeChunk : ImcLin.codeChunks()) {
            genFunction(codeChunk, dataLabels);
        }

        // Main entry point
        writer.groupStart("(func (export \"_start\") (result i64)");
        writer.println("(call $_main)");
        writer.groupEnd();

        writer.groupEnd(); // End module
    }

    private void genStdLib() {
        // Use groupStart to let WatWriter handle formatting
        writer.groupStart("(func $_putchar (param $c i64) (result i64)");
        writer.println("(call $putchar (i32.wrap_i64 (local.get $c)))");
        writer.println("(i64.const 0)");
        writer.groupEnd();

        writer.groupStart("(func $_getchar (result i64)");
        writer.println("(i64.extend_i32_s (call $getchar))");
        writer.groupEnd();

        writer.groupStart("(func $_putint (param $n i64) (result i64)");
        writer.println("(local $val i64)");
        writer.println("(local.set $val (local.get $n))");
        writer.groupStart("(if (i64.lt_s (local.get $val) (i64.const 0))");
        writer.groupStart("(then");
        writer.println("(call $putchar (i32.const 45))");
        writer.println("(local.set $val (i64.sub (i64.const 0) (local.get $val)))");
        writer.groupEnd();
        writer.groupEnd();
        writer.groupStart("(if (i64.ge_s (local.get $val) (i64.const 10))");
        writer.groupStart("(then");
        writer.println("(drop (call $_putint (i64.div_s (local.get $val) (i64.const 10))))");
        writer.groupEnd();
        writer.groupEnd();
        writer.println("(call $putchar (i32.wrap_i64 (i64.add (i64.rem_s (local.get $val) (i64.const 10)) (i64.const 48))))");
        writer.println("(i64.const 0)");
        writer.groupEnd();

        writer.groupStart("(func $_new (param $size i64) (result i64)");
        writer.println("(local $addr i64)");
        writer.println("(local.set $addr (global.get $HP))");
        writer.println("(global.set $HP (i64.add (global.get $HP) (local.get $size)))");
        writer.println("(local.get $addr)");
        writer.groupEnd();

        writer.groupStart("(func $_exit (param $code i64) (result i64)");
        writer.println("(call $exit (i32.wrap_i64 (local.get $code)))");
        writer.println("(i64.const 0)");
        writer.groupEnd();
    }

    /**
     * Maps LANG'24 Semantic Types to WebAssembly Text types.
     */
    private String toWasmResult(SemType type) {
        if (type instanceof SemVoidType) {
            return ""; // No result clause for void
        }

        // In LANG'24, bool, char, int, and pointers are all
        // handled as 64-bit integers on the stack.
        return "(result i64)";
    }

    private void genFunction(LinCodeChunk codeChunk, Map<MemLabel, Long> dataLabels) {
        MemFrame frame = codeChunk.frame();
        String resultClause = toWasmResult(frame.resultType);

        writer.println(""); // Blank line for readability
        writer.groupStart("(func $%s %s", frame.label.name(), resultClause);

        // Locals
        Set<MemTemp> temps = discoverTemps(codeChunk);
        for (MemTemp temp : temps) {
            writer.println("(local $T%d i64)", temp.temp);
        }
        writer.println("(local $target i32)");

        // Prologue (Operational Semantics)
        writer.println(";; --- Prologue ---");
        writer.println("(local.set $T%d (global.get $SP))", frame.FP.temp);
        writer.println("(global.set $SP (i64.sub (global.get $SP) (i64.const %d)))", frame.size);

        // Dispatcher logic
        List<ImcStmt> stmts = codeChunk.stmts();
        Map<MemLabel, Integer> labelToIndex = new HashMap<>();
        List<MemLabel> labels = new ArrayList<>();

        for (ImcStmt s : stmts) {
            if (s instanceof ImcLABEL l) {
                labelToIndex.put(l.label, labels.size());
                labels.add(l.label);
            }
        }

        int entryIdx = labelToIndex.getOrDefault(codeChunk.entryLabel(), 0);
        writer.println("(local.set $target (i32.const %d))", entryIdx);

        writer.groupStart("(loop $L");
        writer.groupStart("(block $B_exit");

        for (int i = labels.size() - 1; i >= 0; i--) {
            writer.groupStart("(block $B_%d", i);
        }

        // br_table must be generated as one instruction
        StringBuilder brTable = new StringBuilder("(br_table ");
        for (int i = 0; i < labels.size(); i++) brTable.append("$B_").append(i).append(" ");
        brTable.append("(local.get $target))");
        writer.println(brTable.toString());

        for (int i = 0; i < labels.size(); i++) {
            writer.groupEnd(); // Close block $B_i
            writer.println(";; Label: %s", labels.get(i).name());

            int stmtIdx = -1;
            for (int j = 0; j < stmts.size(); j++) {
                if (stmts.get(j) instanceof ImcLABEL l && l.label.equals(labels.get(i))) {
                    stmtIdx = j + 1;
                    break;
                }
            }

            if (stmtIdx != -1) {
                WatVisitor visitor = new WatVisitor(writer, dataLabels, labelToIndex);
                while (stmtIdx < stmts.size()) {
                    ImcStmt stmt = stmts.get(stmtIdx);
                    if (stmt instanceof ImcLABEL) break;
                    stmt.accept(visitor, null);
                    if (stmt instanceof ImcJUMP || stmt instanceof ImcCJUMP) break;
                    stmtIdx++;
                }

                // Fallthrough handling
                if (stmtIdx < stmts.size() && stmts.get(stmtIdx) instanceof ImcLABEL nextLabel) {
                    writer.println("(local.set $target (i32.const %d))", labelToIndex.get(nextLabel.label));
                    writer.println("(br $L)");
                }
            }
        }

        writer.groupEnd(); // Close $B_exit
        writer.groupEnd(); // Close $L

        // Epilogue
        writer.println(";; --- Epilogue ---");
        writer.println("(global.set $SP (i64.add (global.get $SP) (i64.const %d)))", frame.size);

        // Only push return value if the type is NOT void
        if (!(frame.resultType instanceof SemVoidType)) {
            writer.println("(local.get $T%d)", frame.RV.temp);
        }

        writer.groupEnd(); // Close func

    }

    private Set<MemTemp> discoverTemps(LinCodeChunk chunk) {
        TempVisitor visitor = new TempVisitor();
        for (ImcStmt stmt : chunk.stmts()) {
            stmt.accept(visitor, null);
        }
        Set<MemTemp> temps = visitor.temps;
        if (chunk.frame().FP != null) temps.add(chunk.frame().FP);
        if (chunk.frame().RV != null) temps.add(chunk.frame().RV);
        return temps;
    }
}
