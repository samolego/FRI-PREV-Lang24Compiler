package lang24.data.ast.visitor;

import lang24.common.report.*;
import lang24.data.ast.tree.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.*;

/**
 * An abstract syntax tree visitor.
 * 
 * @param <Result>   The result type.
 * @param <Argument> The argument type.
 *
 * @author bostjan.slivnik@fri.uni-lj.si
 */
public interface AstVisitor<Result, Argument> {

	// lang24.data.ast.tree:

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstNodes<? extends AstNode> nodes, Argument arg);

	// lang24.data.ast.tree.defn:

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstTypDefn typDefn, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstVarDefn varDefn, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstFunDefn funDefn, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstFunDefn.AstRefParDefn refParDefn, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstFunDefn.AstValParDefn valParDefn, Argument arg);

	// lang24.data.ast.tree.expr:

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstArrExpr arrExpr, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstAtomExpr atomExpr, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstBinExpr binExpr, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstCallExpr callExpr, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstCastExpr castExpr, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstCmpExpr cmpExpr, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstNameExpr nameExpr, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstPfxExpr pfxExpr, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstSfxExpr sfxExpr, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstSizeofExpr sizeofExpr, Argument arg);

	// lang24.data.ast.tree.stmt:

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstAssignStmt assignStmt, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstBlockStmt blockStmt, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstExprStmt exprStmt, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstIfStmt ifStmt, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstReturnStmt retStmt, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstWhileStmt whileStmt, Argument arg);

	// lang24.data.ast.tree.type:

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstArrType arrType, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstAtomType atomType, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstNameType nameType, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstPtrType ptrType, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstStrType strType, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstUniType uniType, Argument arg);

	@SuppressWarnings({ "doclint:missing" })
	Result visit(AstRecType.AstCmpDefn cmpDefn, Argument arg);

}