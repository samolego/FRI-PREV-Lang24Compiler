package lang24.phase.memory;

import lang24.common.report.Report;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.*;
import lang24.data.mem.*;
import lang24.data.type.*;
import lang24.phase.seman.SemAn;

/**
 * Computing memory layout: stack frames and variable accesses.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class MemEvaluator implements AstFullVisitor<Long, Integer> {

    /**
     * Gets the size of a type in bytes.
     * @param type The type to get the size of.
     * @return The size of the type in bytes.
     */
    private static long getSizeInBytes(SemType type) {
        return switch (type) {
            case SemPointerType ignored -> 8;
            case SemIntType ignored -> 8;
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
                    size = Math.max(size, getSizeInBytes(cmp));
                }
                yield size;
            }
            case SemArrayType semArrayType -> semArrayType.size * getSizeInBytes(semArrayType.elemType);
            case null, default -> throw new Report.InternalError();
        };
    }


    /**
     * Get the size of a type in bytes, rounded up to the nearest multiple of 8.
     * @param type The type to get the size of.
     * @return The size of the type in bytes, rounded up to the nearest multiple of 8.
     */
    private static long getRoundedSizeInBytes(SemType type) {
        long size = getSizeInBytes(type);
        if (size % 8 != 0) {
            size += 8 - size % 8;
        }

        return size;
    }

    @Override
    public Long visit(AstNodes<? extends AstNode> nodes, Integer depth) {
        if (depth == null) {
            depth = 0;
        }
        return AstFullVisitor.super.visit(nodes, depth);
    }

    @Override
    public Long visit(AstTypDefn typDefn, Integer depth) {
        return AstFullVisitor.super.visit(typDefn, depth);
    }

    @Override
    public Long visit(AstVarDefn varDefn, Integer depth) {
        // Get type of the variable
        var type = SemAn.ofType.get(varDefn);

        // Get the size of the type in bytes
        long size = getRoundedSizeInBytes(type);

        MemAccess memAcc;
        if (depth == 0) {
            // Static variable definition
            memAcc = new MemAbsAccess(size, new MemLabel(varDefn.name));
        } else {
            memAcc = new MemRelAccess(size, 0, depth);
        }

        Memory.varAccesses.put(varDefn, memAcc);

        // Shouldn't need? todo
        varDefn.type.accept(this, depth);


        return size;
    }

    @Override
    public Long visit(AstFunDefn funDefn, Integer depth) {
        long argsSize = funDefn.pars.accept(this, depth + 1);
        funDefn.defns.accept(this, depth + 1);

        long blockSize = 0;
        if (funDefn.stmt != null) {
            blockSize = funDefn.stmt.accept(this, depth + 1);
        }

        long size = argsSize + blockSize;
        var frame = new MemFrame(new MemLabel(funDefn.name), depth, blockSize, argsSize, size);
        Memory.frames.put(funDefn, frame);


        return null;
    }

    @Override
    public Long visit(AstFunDefn.AstRefParDefn refParDefn, Integer depth) {
        return AstFullVisitor.super.visit(refParDefn, depth);
    }

    @Override
    public Long visit(AstFunDefn.AstValParDefn valParDefn, Integer depth) {
        return AstFullVisitor.super.visit(valParDefn, depth);
    }

    @Override
    public Long visit(AstArrExpr arrExpr, Integer depth) {
        return AstFullVisitor.super.visit(arrExpr, depth);
    }

    @Override
    public Long visit(AstAtomExpr atomExpr, Integer depth) {
        return AstFullVisitor.super.visit(atomExpr, depth);
    }

    @Override
    public Long visit(AstBinExpr binExpr, Integer depth) {
        return AstFullVisitor.super.visit(binExpr, depth);
    }

    @Override
    public Long visit(AstCallExpr callExpr, Integer depth) {
        return AstFullVisitor.super.visit(callExpr, depth);
    }

    @Override
    public Long visit(AstCastExpr castExpr, Integer depth) {
        return AstFullVisitor.super.visit(castExpr, depth);
    }

    @Override
    public Long visit(AstCmpExpr cmpExpr, Integer depth) {
        return AstFullVisitor.super.visit(cmpExpr, depth);
    }

    @Override
    public Long visit(AstNameExpr nameExpr, Integer depth) {
        return AstFullVisitor.super.visit(nameExpr, depth);
    }

    @Override
    public Long visit(AstPfxExpr pfxExpr, Integer depth) {
        return AstFullVisitor.super.visit(pfxExpr, depth);
    }

    @Override
    public Long visit(AstSfxExpr sfxExpr, Integer depth) {
        return AstFullVisitor.super.visit(sfxExpr, depth);
    }

    @Override
    public Long visit(AstSizeofExpr sizeofExpr, Integer depth) {
        return AstFullVisitor.super.visit(sizeofExpr, depth);
    }

    @Override
    public Long visit(AstAssignStmt assignStmt, Integer depth) {
        return AstFullVisitor.super.visit(assignStmt, depth);
    }

    @Override
    public Long visit(AstBlockStmt blockStmt, Integer depth) {
        return AstFullVisitor.super.visit(blockStmt, depth);
    }

    @Override
    public Long visit(AstExprStmt exprStmt, Integer depth) {
        return AstFullVisitor.super.visit(exprStmt, depth);
    }

    @Override
    public Long visit(AstIfStmt ifStmt, Integer depth) {
        return AstFullVisitor.super.visit(ifStmt, depth);
    }

    @Override
    public Long visit(AstReturnStmt retStmt, Integer depth) {
        return AstFullVisitor.super.visit(retStmt, depth);
    }

    @Override
    public Long visit(AstWhileStmt whileStmt, Integer depth) {
        return AstFullVisitor.super.visit(whileStmt, depth);
    }

    @Override
    public Long visit(AstArrType arrType, Integer depth) {
        return AstFullVisitor.super.visit(arrType, depth);
    }

    @Override
    public Long visit(AstAtomType atomType, Integer depth) {
        return AstFullVisitor.super.visit(atomType, depth);
    }

    @Override
    public Long visit(AstNameType nameType, Integer depth) {
        return AstFullVisitor.super.visit(nameType, depth);
    }

    @Override
    public Long visit(AstPtrType ptrType, Integer depth) {
        return AstFullVisitor.super.visit(ptrType, depth);
    }

    @Override
    public Long visit(AstStrType strType, Integer depth) {
        return AstFullVisitor.super.visit(strType, depth);
    }

    @Override
    public Long visit(AstUniType uniType, Integer depth) {
        return AstFullVisitor.super.visit(uniType, depth);
    }

    @Override
    public Long visit(AstRecType.AstCmpDefn cmpDefn, Integer depth) {
        return AstFullVisitor.super.visit(cmpDefn, depth);
    }
}
