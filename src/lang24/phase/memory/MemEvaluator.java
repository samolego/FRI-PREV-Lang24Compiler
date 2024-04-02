package lang24.phase.memory;

import lang24.common.report.Report;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.*;
import lang24.data.mem.*;
import lang24.data.type.*;
import lang24.phase.lexan.LexAn;
import lang24.phase.seman.NameResolver;
import lang24.phase.seman.SemAn;
import lang24.phase.synan.SynAn;

import static java.lang.Math.max;


/**
 * Computing memory layout: stack frames and variable accesses.
 *
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class MemEvaluator implements AstFullVisitor<Object, Integer> {

    private long maxCallSize = getSizeInBytes(SemPointerType.type);  // Static link always included

    /**
     * Gets the size of a type in bytes.
     *
     * @param type The type to get the size of.
     * @return The size of the type in bytes.
     */
    private static long getSizeInBytes(SemType type) {
        return switch (type) {
            case SemPointerType ignored -> 8;
            case SemIntType ignored -> 4;
            case SemBoolType ignored -> 1;
            case SemCharType ignored -> 1;
            case SemStructType semStructType -> {
                long size = 0;
                for (var cmp : semStructType.cmpTypes) {
                    size += getSizeInBytes(cmp);
                }
                yield size;
            }
            case SemUnionType semUnionType -> {
                long size = 0;
                for (var cmp : semUnionType.cmpTypes) {
                    size = max(size, getSizeInBytes(cmp));
                }
                yield size;
            }
            case SemArrayType semArrayType -> semArrayType.size * getSizeInBytes(semArrayType.elemType);
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
        return roundTo8(size);
    }

    private static long roundTo8(long size) {
        if (size % 8 != 0) {
            size += 8 - size % 8;
        }

        return size;
    }

    @Override
    public Object visit(AstNodes<? extends AstNode> nodes, Integer depth) {
        if (depth == null) {
            depth = 0;
        }
        return AstFullVisitor.super.visit(nodes, depth);
    }


    @Override
    public Object visit(AstVarDefn varDefn, Integer depth) {
        // Get type of the variable
        var type = SemAn.ofType.get(varDefn);

        // Get the size of the type in bytes
        long size = getSizeInBytes(type);

        if (depth == 0) {
            // Static variable definition
            var memAcc = new MemAbsAccess(size, new MemLabel(varDefn.name));
            Memory.varAccesses.put(varDefn, memAcc);
        }

        varDefn.type.accept(this, depth);

        return size;
    }

    @Override
    public Object visit(AstFunDefn funDefn, Integer depth) {

        long paramSize = 0;
        for (var par : funDefn.pars) {
            par.accept(this, depth);

            var type = SemAn.ofType.get(par);
            // Round parameters & arguments
            long size = getRoundedSizeInBytes(type);

            var memAcc = new MemRelAccess(size, paramSize, depth);
            Memory.parAccesses.put(par, memAcc);

            paramSize += size;
        }


        long blockSize = 0;
        for (var defn : funDefn.defns) {
            defn.accept(this, depth);

            if (defn instanceof AstVarDefn varDefn) {
                var type = SemAn.ofType.get(defn);
                long size = getSizeInBytes(type);
                blockSize += size;

                // Just variables interest us
                // Automatic variable definition
                var memAcc = new MemRelAccess(size, -blockSize, depth);

                Memory.varAccesses.put(varDefn, memAcc);
            }
        }


        this.maxCallSize = getSizeInBytes(SemPointerType.type);
        if (funDefn.stmt != null) {
            funDefn.stmt.accept(this, depth + 1);
        }

        long size = blockSize + this.maxCallSize + 2 * getSizeInBytes(SemPointerType.type); // one for return address, one for old frame pointer

        boolean isNested = depth > 0;
        MemLabel label = isNested ? new MemLabel() : new MemLabel(funDefn.name());

        var frame = new MemFrame(label, depth, blockSize, this.maxCallSize, size);

        Memory.frames.put(funDefn, frame);


        return size;
    }

    // todo
    @Override
    public Object visit(AstAtomExpr atomExpr, Integer depth) {
        if (atomExpr.type == AstAtomExpr.Type.STR) {
            assert SemAn.ofType.get(atomExpr) == SemPointerType.stringType : "Wrong string pointer for node " + atomExpr.getText();
            Memory.strings.put(atomExpr, new MemAbsAccess(8, new MemLabel(), atomExpr.value));
        }

        return null;
    }

    @Override
    public Object visit(AstCallExpr callExpr, Integer depth) {
        //var fnDefn = SemAn.definedAt.get(callExpr);
        long argSize = getSizeInBytes(SemPointerType.type); // We need at least static link for each function
        for (var arg : callExpr.args) {
            arg.accept(this, depth);

            var type = SemAn.ofType.get(arg);
            // Round parameters & arguments
            long size = getRoundedSizeInBytes(type);
            argSize += size;
        }

        // todo - ask - should we round the return size as well?
        long returnSize = getRoundedSizeInBytes(SemAn.ofType.get(callExpr));

        long callSize = max(argSize, returnSize);

        this.maxCallSize = max(this.maxCallSize, callSize);

        return null;
    }


    @Override
    public Object visit(AstStrType strType, Integer depth) {
        return processRecordType(strType);
    }

    @Override
    public Object visit(AstUniType uniType, Integer depth) {
        return processRecordType(uniType);
    }

    private long processRecordType(AstRecType recType) {
        long offset = 0;
        for (var cmp : recType.cmps) {
            cmp.accept(this, -1);

            var type = SemAn.ofType.get(cmp);
            // Round the size in components
            long size = getRoundedSizeInBytes(type);

            // Depth is -1 for components of a record
            long cmpOffset = recType instanceof AstStrType ? offset : 0;
            var memAcc = new MemRelAccess(size, cmpOffset, -1);
            Memory.cmpAccesses.put(cmp, memAcc);

            offset += size;
        }

        return offset;
    }
}
