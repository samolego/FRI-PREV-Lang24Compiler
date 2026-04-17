package lang24.phase.watgen;

import lang24.common.report.Report;
import lang24.data.ast.tree.defn.AstFunDefn.AstParDefn;
import lang24.data.imc.code.stmt.*;
import lang24.data.lin.LinCodeChunk;
import lang24.data.lin.LinDataChunk;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemTemp;
import lang24.data.mem.MemFrame;
import lang24.data.type.SemType;
import lang24.data.type.SemVoidType;
import lang24.data.type.WatType;
import lang24.data.type.WatType.Type;
import lang24.data.wat.TempVisitor;
import lang24.data.wat.WatVisitor;
import lang24.data.wat.WatWriter;
import lang24.phase.Phase;
import lang24.phase.imclin.ImcLin;
import lang24.phase.memory.Memory;
import lang24.phase.seman.SemAn;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class WatGen extends Phase {

    private final WatWriter writer;

    private static final long HEAP_START = 0x10000;
    private static final long STACK_START = 0x800000;

    public WatGen(String filename) {
        super("watgen");
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

        // External imports
        for (var fn : Memory.externalFns.entrySet()) {
            var functionName = fn.getKey().name();
            var astFnDefn = fn.getValue();
            var wt = (WatType) SemAn.ofType.get(astFnDefn.type.parent);
            var watType = wt.watType();

            StringBuilder paramsBuilder = new StringBuilder(" (param");
            for (AstParDefn par : astFnDefn.pars) {
                var p = (WatType) SemAn.ofType.get(par);
                paramsBuilder.append(" ").append(p.watType().toString());
            }
            String paramsStr;
            if (astFnDefn.pars.size() > 0) {
                paramsStr = paramsBuilder.append(")").toString();
            } else {
                paramsStr = "";
            }

            var result = "";
            if (watType != Type.VOID) {
                result = " (result %s)".formatted(watType.toString());
            }

            // Module-imports (split by _)
            // Defaulting to env
            String moduleName = "env";
            String cleanName = functionName.substring(1);
            String fieldName = cleanName;
            if (cleanName.contains("_")) {
                int idx = cleanName.indexOf("_");
                moduleName = cleanName.substring(0, idx);
                fieldName = cleanName.substring(idx + 1);
            }

            // Import external function
            writer.println("(import \"%s\" \"%s\" (func $%s%s%s))", moduleName, fieldName, functionName, paramsStr, result);
        }

        writer.println("(memory (export \"memory\") 200)");

        writer.println("(global $SP (mut i64) (i64.const %d))", STACK_START);
        writer.println("(global $HP (mut i64) (i64.const %d))", HEAP_START);

        // Data chunks
        long currentDataOffset = 1024;
        Map<MemLabel, Long> dataLabels = new HashMap<>();
        for (LinDataChunk dataChunk : ImcLin.dataChunks()) {
            dataLabels.put(dataChunk.label, currentDataOffset);
            if (dataChunk.init != null) {
                // Our char is defined as 8 BITS,
                // so we must add padding with 0
                StringBuilder hex = new StringBuilder();
                for (char c : dataChunk.init.toCharArray()) {
                    hex.append(String.format("\\%02x\\00\\00\\00\\00\\00\\00\\00", (int) c));
                }
                hex.append("\\00\\00\\00\\00\\00\\00\\00\\00"); // Null terminator
                writer.println("(data (i32.const %d) \"%s\")", currentDataOffset, hex.toString());
            }
            currentDataOffset += dataChunk.size;
            currentDataOffset = (currentDataOffset + 0b0111) & ~0b0111;
        }

        //WatStdLib.genStdLib(writer);

        for (LinCodeChunk codeChunk : ImcLin.codeChunks()) {
            genFunction(codeChunk, dataLabels);
        }

        // Main entry point
        writer.groupStart("(func (export \"main\") (result i64)");
        writer.println("(call $_main)");
        writer.groupEnd();

        writer.groupEnd(); // End module
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

        for (MemLabel label : labels) {
            writer.groupEnd(); // Close block $B_i
            writer.println(";; Label: %s", label.name());

            int stmtIdx = -1;
            for (int j = 0; j < stmts.size(); j++) {
                if (stmts.get(j) instanceof ImcLABEL l && l.label.equals(label)) {
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
