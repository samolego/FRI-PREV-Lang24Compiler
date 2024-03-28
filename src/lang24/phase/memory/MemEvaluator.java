package lang24.phase.memory;

import java.util.*;

import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.visitor.*;
import lang24.data.mem.*;
import lang24.data.type.*;
import lang24.data.type.visitor.*;
import lang24.phase.seman.SemAn;

/**
 * Computing memory layout: stack frames and variable accesses.
 * 
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public class MemEvaluator implements AstFullVisitor<Object, Object> {

    @Override
    public Object visit(AstNodes<? extends AstNode> nodes, Object arg) {
        return AstFullVisitor.super.visit(nodes, arg);
    }

    @Override
    public Object visit(AstTypDefn typDefn, Object arg) {
        return AstFullVisitor.super.visit(typDefn, arg);
    }

    @Override
    public Object visit(AstVarDefn varDefn, Object arg) {
        return AstFullVisitor.super.visit(varDefn, arg);
    }

    @Override
    public Object visit(AstFunDefn funDefn, Object arg) {
        Memory.frames.put(funDefn, new MemFrame(funDefn));
        return AstFullVisitor.super.visit(funDefn, arg);
    }

    @Override
    public Object visit(AstFunDefn.AstRefParDefn refParDefn, Object arg) {
        return AstFullVisitor.super.visit(refParDefn, arg);
    }

    @Override
    public Object visit(AstFunDefn.AstValParDefn valParDefn, Object arg) {
        return AstFullVisitor.super.visit(valParDefn, arg);
    }

    @Override
    public Object visit(AstArrExpr arrExpr, Object arg) {
        return AstFullVisitor.super.visit(arrExpr, arg);
    }

    @Override
    public Object visit(AstAtomExpr atomExpr, Object arg) {
        return AstFullVisitor.super.visit(atomExpr, arg);
    }

    @Override
    public Object visit(AstBinExpr binExpr, Object arg) {
        return AstFullVisitor.super.visit(binExpr, arg);
    }

    @Override
    public Object visit(AstCallExpr callExpr, Object arg) {
        return AstFullVisitor.super.visit(callExpr, arg);
    }

    @Override
    public Object visit(AstCastExpr castExpr, Object arg) {
        return AstFullVisitor.super.visit(castExpr, arg);
    }

    @Override
    public Object visit(AstCmpExpr cmpExpr, Object arg) {
        return AstFullVisitor.super.visit(cmpExpr, arg);
    }

    @Override
    public Object visit(AstNameExpr nameExpr, Object arg) {
        return AstFullVisitor.super.visit(nameExpr, arg);
    }

    @Override
    public Object visit(AstPfxExpr pfxExpr, Object arg) {
        return AstFullVisitor.super.visit(pfxExpr, arg);
    }

    @Override
    public Object visit(AstSfxExpr sfxExpr, Object arg) {
        return AstFullVisitor.super.visit(sfxExpr, arg);
    }

    @Override
    public Object visit(AstSizeofExpr sizeofExpr, Object arg) {
        return AstFullVisitor.super.visit(sizeofExpr, arg);
    }

    @Override
    public Object visit(AstAssignStmt assignStmt, Object arg) {
        return AstFullVisitor.super.visit(assignStmt, arg);
    }

    @Override
    public Object visit(AstBlockStmt blockStmt, Object arg) {
        return AstFullVisitor.super.visit(blockStmt, arg);
    }

    @Override
    public Object visit(AstExprStmt exprStmt, Object arg) {
        return AstFullVisitor.super.visit(exprStmt, arg);
    }

    @Override
    public Object visit(AstIfStmt ifStmt, Object arg) {
        return AstFullVisitor.super.visit(ifStmt, arg);
    }

    @Override
    public Object visit(AstReturnStmt retStmt, Object arg) {
        return AstFullVisitor.super.visit(retStmt, arg);
    }

    @Override
    public Object visit(AstWhileStmt whileStmt, Object arg) {
        return AstFullVisitor.super.visit(whileStmt, arg);
    }

    @Override
    public Object visit(AstArrType arrType, Object arg) {
        return AstFullVisitor.super.visit(arrType, arg);
    }

    @Override
    public Object visit(AstAtomType atomType, Object arg) {
        return AstFullVisitor.super.visit(atomType, arg);
    }

    @Override
    public Object visit(AstNameType nameType, Object arg) {
        return AstFullVisitor.super.visit(nameType, arg);
    }

    @Override
    public Object visit(AstPtrType ptrType, Object arg) {
        return AstFullVisitor.super.visit(ptrType, arg);
    }

    @Override
    public Object visit(AstStrType strType, Object arg) {
        return AstFullVisitor.super.visit(strType, arg);
    }

    @Override
    public Object visit(AstUniType uniType, Object arg) {
        return AstFullVisitor.super.visit(uniType, arg);
    }

    @Override
    public Object visit(AstRecType.AstCmpDefn cmpDefn, Object arg) {
        return AstFullVisitor.super.visit(cmpDefn, arg);
    }
}
