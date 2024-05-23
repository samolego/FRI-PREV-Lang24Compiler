package lang24.phase.memory;

import lang24.common.report.Report;
import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.AstAtomExpr;
import lang24.data.ast.tree.expr.AstCallExpr;
import lang24.data.ast.tree.type.AstRecType;
import lang24.data.ast.tree.type.AstStrType;
import lang24.data.ast.tree.type.AstUniType;
import lang24.data.ast.visitor.AstFullVisitor;
import lang24.data.mem.MemAbsAccess;
import lang24.data.mem.MemFrame;
import lang24.data.mem.MemLabel;
import lang24.data.mem.MemRelAccess;
import lang24.data.type.SemArrayType;
import lang24.data.type.SemBoolType;
import lang24.data.type.SemCharType;
import lang24.data.type.SemIntType;
import lang24.data.type.SemPointerType;
import lang24.data.type.SemStructType;
import lang24.data.type.SemType;
import lang24.data.type.SemUnionType;
import lang24.data.type.SemVoidType;
import lang24.phase.seman.SemAn;

import java.util.regex.Pattern;

import static java.lang.Math.max;


/**
 * Computing memory layout: stack frames and variable accesses.
 *
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class MemEvaluator implements AstFullVisitor<Void, Integer> {
    /**
     * Size of static link in bytes.
     */
    public static final long POINTER_SIZE = getSizeInBytes(SemPointerType.type);
    /**
     * Regex pattern for matching hex numbers (\ hex hex).
     */
    private static final Pattern HEX_REGEX_PATTERN = Pattern.compile("\\\\[0-9A-F]{2}");
    private static final boolean DO_HEX_ESC = Boolean.parseBoolean(System.getenv().getOrDefault("DO_HEX_ESC", "false"));

    /**
     * The maximum size of latest function call (max of arguments + SL, return value).
     */
    private long maxCallSize = POINTER_SIZE;  // Static link always included

    /**
     * Gets the size of a type in bytes.
     *
     * @param type The type to get the size of.
     * @return The size of the type in bytes.
     */
    public static long getSizeInBytes(SemType type) {
        return switch (type) {
            case SemVoidType ignored -> 0;
            case SemPointerType ignored -> 8;
            case SemIntType ignored -> 8;
            case SemBoolType ignored -> 8;
            case SemCharType ignored -> 8;
            case SemStructType semStructType -> {
                long size = 0;
                for (var cmp : semStructType.cmpTypes) {
                    size += getRoundedSizeInBytes(cmp);
                }
                yield size;
            }
            case SemUnionType semUnionType -> {
                long size = 0;
                for (var cmp : semUnionType.cmpTypes) {
                    size = max(size, getRoundedSizeInBytes(cmp));
                }
                yield size;
            }
            // Array elements are always aligned to 8 bytes
            case SemArrayType semArrayType -> semArrayType.size * getRoundedSizeInBytes(semArrayType.elemType);
            case null, default -> throw new Report.InternalError();
        };
    }


    /**
     * Get the size of a type in bytes, rounded up to the nearest multiple of 8.
     *
     * @param type The type to get the size of.
     * @return The size of the type in bytes, rounded up to the nearest multiple of 8.
     */
    private static long getRoundedSizeInBytes(SemType type) {
        long size = getSizeInBytes(type);
        return ceilTo8(size);
    }

    /**
     * Ceils a long up to the nearest multiple of 8.
     *
     * @param size the long to round
     * @return
     */
    private static long ceilTo8(long size) {
        return 0xFFFF_FFFF_FFFF_FFF8L & (size + 7);
    }

    @Override
    public Void visit(AstNodes<? extends AstNode> nodes, Integer depth) {
        if (depth == null) {
            // Root node
            depth = 0;
        }
        return AstFullVisitor.super.visit(nodes, depth);
    }


    @Override
    public Void visit(AstVarDefn varDefn, Integer depth) {
        // Get type of the variable
        var type = SemAn.ofType.get(varDefn);

        // Get the size of the type in bytes
        long size = getSizeInBytes(type);

        if (depth == 0) {
            // Static variable definition
            var memAcc = new MemAbsAccess(size, new MemLabel(varDefn.name));
            Memory.varAccesses.put(varDefn, memAcc);
        }  // Otherwise it is an automatic variable definition, so it's handled in AstFunDefn

        varDefn.type.accept(this, depth);

        return null;
    }

    @Override
    public Void visit(AstFunDefn funDefn, Integer depth) {
        this.maxCallSize = POINTER_SIZE;

        long paramSize = 0;
        for (var par : funDefn.pars) {
            par.accept(this, depth);

            var type = SemAn.ofType.get(par);
            // Round parameters & arguments
            long size = getSizeInBytes(type);

            var memAcc = new MemRelAccess(size, paramSize + POINTER_SIZE, depth);
            Memory.parAccesses.put(par, memAcc);

            paramSize += ceilTo8(size);
        }

        long localSize = 0;
        for (var defn : funDefn.defns) {
            if (defn instanceof AstFunDefn) {
                defn.accept(this, depth + 1);
            } else {
                defn.accept(this, depth);
            }

            // Just variables interest us
            if (defn instanceof AstVarDefn varDefn) {
                var type = SemAn.ofType.get(defn);
                long size = getSizeInBytes(type);
                localSize += ceilTo8(size);

                // Automatic variable definition
                var memAcc = new MemRelAccess(size, -localSize, depth);

                Memory.varAccesses.put(varDefn, memAcc);
            }
        }

        if (funDefn.stmt != null) {

            funDefn.stmt.accept(this, depth + 1);
            long maxOfArgsAndReturn = this.maxCallSize;

            long size = localSize + maxOfArgsAndReturn + 2 * getSizeInBytes(SemPointerType.type); // one for return address, one for old frame pointer

            boolean isNested = depth > 0;
            MemLabel label = isNested ? new MemLabel() : new MemLabel(funDefn.name());

            var frame = new MemFrame(label, depth, localSize, maxOfArgsAndReturn, size);
            Memory.frames.put(funDefn, frame);
        }


        return null;
    }

    @Override
    public Void visit(AstAtomExpr atomExpr, Integer depth) {
        if (atomExpr.type == AstAtomExpr.Type.STR) {
            assert SemAn.ofType.get(atomExpr) == SemPointerType.stringType : "Wrong string pointer for node " + atomExpr.getText();

            var parsedStr = atomExpr.value
                    .substring(1, atomExpr.value.length() - 1)  // Remove quotes
                    .replaceAll("\\\\n", "\n")  // Replace \n with newline
                    .replaceAll("\\\\\\\\", "\\\\")  // Replace \\ with \
                    .replaceAll("\\\\\"", "\"");  // Replace \" with "


            if (DO_HEX_ESC) {
                var hexMatcher = HEX_REGEX_PATTERN.matcher(parsedStr);

                while (hexMatcher.find()) {
                    var hex = hexMatcher.group().substring(1);
                    char charValue = (char) Integer.parseInt(hex, 16);
                    parsedStr = parsedStr.replace(hexMatcher.group(), String.valueOf(charValue));
                }
            }
            long length = parsedStr.length();
            Memory.strings.put(atomExpr, new MemAbsAccess(getSizeInBytes(SemCharType.type) * length, new MemLabel(), parsedStr));
        }

        return null;
    }

    @Override
    public Void visit(AstCallExpr callExpr, Integer depth) {
        long argSize = POINTER_SIZE; // We need at least static link for each function
        for (var arg : callExpr.args) {
            arg.accept(this, depth);

            var type = SemAn.ofType.get(arg);
            // Round parameters & arguments
            long size = getRoundedSizeInBytes(type);
            argSize += size;
        }

        // We also round the return variable
        long returnSize = getRoundedSizeInBytes(SemAn.ofType.get(callExpr));
        long callSize = max(argSize, returnSize);

        this.maxCallSize = max(this.maxCallSize, callSize);

        return null;
    }


    @Override
    public Void visit(AstStrType strType, Integer depth) {
        processRecordType(strType);
        return null;
    }

    @Override
    public Void visit(AstUniType uniType, Integer depth) {
        processRecordType(uniType);
        return null;
    }

    private void processRecordType(AstRecType recType) {
        long offset = 0;
        for (var cmp : recType.cmps) {
            cmp.accept(this, -1);

            var type = SemAn.ofType.get(cmp);
            long size = getSizeInBytes(type);

            // Depth is -1 for components of a record
            long cmpOffset = recType instanceof AstStrType ? offset : 0;
            var memAcc = new MemRelAccess(size, cmpOffset, -1);
            Memory.cmpAccesses.put(cmp, memAcc);

            offset += ceilTo8(size);
        }
    }
}
