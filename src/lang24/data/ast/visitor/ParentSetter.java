package lang24.data.ast.visitor;

import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;
import lang24.data.ast.tree.defn.AstFunDefn;
import lang24.data.ast.tree.defn.AstTypDefn;
import lang24.data.ast.tree.defn.AstVarDefn;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.*;

public class ParentSetter implements AstFullVisitor<Object, AstNode> {

    @Override
    public Object visit(AstNodes<? extends AstNode> nodes, AstNode parent) {
        nodes.parent = parent;
        return AstFullVisitor.super.visit(nodes, nodes);
    }

    @Override
    public Object visit(AstTypDefn typDefn, AstNode parent) {
        typDefn.parent = parent;
        return AstFullVisitor.super.visit(typDefn, typDefn);
    }

    @Override
    public Object visit(AstVarDefn varDefn, AstNode parent) {
        varDefn.parent = parent;
        return AstFullVisitor.super.visit(varDefn, varDefn);
    }

    @Override
    public Object visit(AstFunDefn funDefn, AstNode parent) {
        funDefn.parent = parent;
        return AstFullVisitor.super.visit(funDefn, funDefn);
    }

    @Override
    public Object visit(AstFunDefn.AstRefParDefn refParDefn, AstNode parent) {
        refParDefn.parent = parent;
        return AstFullVisitor.super.visit(refParDefn, refParDefn);
    }

    @Override
    public Object visit(AstFunDefn.AstValParDefn valParDefn, AstNode parent) {
        valParDefn.parent = parent;
        return AstFullVisitor.super.visit(valParDefn, valParDefn);
    }

    @Override
    public Object visit(AstArrExpr arrExpr, AstNode parent) {
        arrExpr.parent = parent;
        return AstFullVisitor.super.visit(arrExpr, arrExpr);
    }

    @Override
    public Object visit(AstAtomExpr atomExpr, AstNode parent) {
        atomExpr.parent = parent;
        return AstFullVisitor.super.visit(atomExpr, atomExpr);
    }

    @Override
    public Object visit(AstBinExpr binExpr, AstNode parent) {
        binExpr.parent = parent;
        return AstFullVisitor.super.visit(binExpr, binExpr);
    }

    @Override
    public Object visit(AstCallExpr callExpr, AstNode parent) {
        callExpr.parent = parent;
        return AstFullVisitor.super.visit(callExpr, callExpr);
    }

    @Override
    public Object visit(AstCastExpr castExpr, AstNode parent) {
        castExpr.parent = parent;
        return AstFullVisitor.super.visit(castExpr, castExpr);
    }

    @Override
    public Object visit(AstCmpExpr cmpExpr, AstNode parent) {
        cmpExpr.parent = parent;
        return AstFullVisitor.super.visit(cmpExpr, cmpExpr);
    }

    @Override
    public Object visit(AstNameExpr nameExpr, AstNode parent) {
        nameExpr.parent = parent;
        return AstFullVisitor.super.visit(nameExpr, nameExpr);
    }

    @Override
    public Object visit(AstPfxExpr pfxExpr, AstNode parent) {
        pfxExpr.parent = parent;
        return AstFullVisitor.super.visit(pfxExpr, pfxExpr);
    }

    @Override
    public Object visit(AstSfxExpr sfxExpr, AstNode parent) {
        sfxExpr.parent = parent;
        return AstFullVisitor.super.visit(sfxExpr, sfxExpr);
    }

    @Override
    public Object visit(AstSizeofExpr sizeofExpr, AstNode parent) {
        sizeofExpr.parent = parent;
        return AstFullVisitor.super.visit(sizeofExpr, sizeofExpr);
    }

    @Override
    public Object visit(AstAssignStmt assignStmt, AstNode parent) {
        assignStmt.parent = parent;
        return AstFullVisitor.super.visit(assignStmt, assignStmt);
    }

    @Override
    public Object visit(AstBlockStmt blockStmt, AstNode parent) {
        blockStmt.parent = parent;
        return AstFullVisitor.super.visit(blockStmt, blockStmt);
    }

    @Override
    public Object visit(AstExprStmt exprStmt, AstNode parent) {
        exprStmt.parent = parent;
        return AstFullVisitor.super.visit(exprStmt, exprStmt);
    }

    @Override
    public Object visit(AstIfStmt ifStmt, AstNode parent) {
        ifStmt.parent = parent;
        return AstFullVisitor.super.visit(ifStmt, ifStmt);
    }

    @Override
    public Object visit(AstReturnStmt retStmt, AstNode parent) {
        retStmt.parent = parent;
        return AstFullVisitor.super.visit(retStmt, retStmt);
    }

    @Override
    public Object visit(AstWhileStmt whileStmt, AstNode parent) {
        whileStmt.parent = parent;
        return AstFullVisitor.super.visit(whileStmt, whileStmt);
    }

    @Override
    public Object visit(AstArrType arrType, AstNode parent) {
        arrType.parent = parent;
        return AstFullVisitor.super.visit(arrType, arrType);
    }

    @Override
    public Object visit(AstAtomType atomType, AstNode parent) {
        atomType.parent = parent;
        return AstFullVisitor.super.visit(atomType, atomType);
    }

    @Override
    public Object visit(AstNameType nameType, AstNode parent) {
        nameType.parent = parent;
        return AstFullVisitor.super.visit(nameType, nameType);
    }

    @Override
    public Object visit(AstPtrType ptrType, AstNode parent) {
        ptrType.parent = parent;
        return AstFullVisitor.super.visit(ptrType, ptrType);
    }

    @Override
    public Object visit(AstStrType strType, AstNode parent) {
        strType.parent = parent;
        return AstFullVisitor.super.visit(strType, strType);
    }

    @Override
    public Object visit(AstUniType uniType, AstNode parent) {
        uniType.parent = parent;
        return AstFullVisitor.super.visit(uniType, uniType);
    }

    @Override
    public Object visit(AstRecType.AstCmpDefn cmpDefn, AstNode parent) {
        cmpDefn.parent = parent;
        return AstFullVisitor.super.visit(cmpDefn, cmpDefn);
    }
}
