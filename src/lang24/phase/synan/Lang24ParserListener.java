// Generated from ../synan/Lang24Parser.g4 by ANTLR 4.13.1

package lang24.phase.synan;

import java.util.*;
import java.util.stream.Collectors;

import lang24.common.report.*;
import lang24.data.token.*;
import lang24.phase.lexan.*;
import lang24.data.ast.tree.expr.*;
import lang24.data.ast.tree.defn.*;
import lang24.data.ast.tree.stmt.*;
import lang24.data.ast.tree.type.*;
import lang24.data.ast.tree.AstNode;
import lang24.data.ast.tree.AstNodes;


import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link Lang24Parser}.
 */
public interface Lang24ParserListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#source}.
	 * @param ctx the parse tree
	 */
	void enterSource(Lang24Parser.SourceContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#source}.
	 * @param ctx the parse tree
	 */
	void exitSource(Lang24Parser.SourceContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#definitions}.
	 * @param ctx the parse tree
	 */
	void enterDefinitions(Lang24Parser.DefinitionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#definitions}.
	 * @param ctx the parse tree
	 */
	void exitDefinitions(Lang24Parser.DefinitionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#type_definition}.
	 * @param ctx the parse tree
	 */
	void enterType_definition(Lang24Parser.Type_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#type_definition}.
	 * @param ctx the parse tree
	 */
	void exitType_definition(Lang24Parser.Type_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#variable_definition}.
	 * @param ctx the parse tree
	 */
	void enterVariable_definition(Lang24Parser.Variable_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#variable_definition}.
	 * @param ctx the parse tree
	 */
	void exitVariable_definition(Lang24Parser.Variable_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#function_definition}.
	 * @param ctx the parse tree
	 */
	void enterFunction_definition(Lang24Parser.Function_definitionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#function_definition}.
	 * @param ctx the parse tree
	 */
	void exitFunction_definition(Lang24Parser.Function_definitionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#parameter}.
	 * @param ctx the parse tree
	 */
	void enterParameter(Lang24Parser.ParameterContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#parameter}.
	 * @param ctx the parse tree
	 */
	void exitParameter(Lang24Parser.ParameterContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#parameters}.
	 * @param ctx the parse tree
	 */
	void enterParameters(Lang24Parser.ParametersContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#parameters}.
	 * @param ctx the parse tree
	 */
	void exitParameters(Lang24Parser.ParametersContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#parameters_list}.
	 * @param ctx the parse tree
	 */
	void enterParameters_list(Lang24Parser.Parameters_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#parameters_list}.
	 * @param ctx the parse tree
	 */
	void exitParameters_list(Lang24Parser.Parameters_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#statement}.
	 * @param ctx the parse tree
	 */
	void enterStatement(Lang24Parser.StatementContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#statement}.
	 * @param ctx the parse tree
	 */
	void exitStatement(Lang24Parser.StatementContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(Lang24Parser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(Lang24Parser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#components}.
	 * @param ctx the parse tree
	 */
	void enterComponents(Lang24Parser.ComponentsContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#components}.
	 * @param ctx the parse tree
	 */
	void exitComponents(Lang24Parser.ComponentsContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#components_list}.
	 * @param ctx the parse tree
	 */
	void enterComponents_list(Lang24Parser.Components_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#components_list}.
	 * @param ctx the parse tree
	 */
	void exitComponents_list(Lang24Parser.Components_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#single_component}.
	 * @param ctx the parse tree
	 */
	void enterSingle_component(Lang24Parser.Single_componentContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#single_component}.
	 * @param ctx the parse tree
	 */
	void exitSingle_component(Lang24Parser.Single_componentContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#intconst}.
	 * @param ctx the parse tree
	 */
	void enterIntconst(Lang24Parser.IntconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#intconst}.
	 * @param ctx the parse tree
	 */
	void exitIntconst(Lang24Parser.IntconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#strconst}.
	 * @param ctx the parse tree
	 */
	void enterStrconst(Lang24Parser.StrconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#strconst}.
	 * @param ctx the parse tree
	 */
	void exitStrconst(Lang24Parser.StrconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#charconst}.
	 * @param ctx the parse tree
	 */
	void enterCharconst(Lang24Parser.CharconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#charconst}.
	 * @param ctx the parse tree
	 */
	void exitCharconst(Lang24Parser.CharconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#boolconst}.
	 * @param ctx the parse tree
	 */
	void enterBoolconst(Lang24Parser.BoolconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#boolconst}.
	 * @param ctx the parse tree
	 */
	void exitBoolconst(Lang24Parser.BoolconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#voidconst}.
	 * @param ctx the parse tree
	 */
	void enterVoidconst(Lang24Parser.VoidconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#voidconst}.
	 * @param ctx the parse tree
	 */
	void exitVoidconst(Lang24Parser.VoidconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#ptrconst}.
	 * @param ctx the parse tree
	 */
	void enterPtrconst(Lang24Parser.PtrconstContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#ptrconst}.
	 * @param ctx the parse tree
	 */
	void exitPtrconst(Lang24Parser.PtrconstContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#atom}.
	 * @param ctx the parse tree
	 */
	void enterAtom(Lang24Parser.AtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#atom}.
	 * @param ctx the parse tree
	 */
	void exitAtom(Lang24Parser.AtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#more_expressions}.
	 * @param ctx the parse tree
	 */
	void enterMore_expressions(Lang24Parser.More_expressionsContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#more_expressions}.
	 * @param ctx the parse tree
	 */
	void exitMore_expressions(Lang24Parser.More_expressionsContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#more_expressions_list}.
	 * @param ctx the parse tree
	 */
	void enterMore_expressions_list(Lang24Parser.More_expressions_listContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#more_expressions_list}.
	 * @param ctx the parse tree
	 */
	void exitMore_expressions_list(Lang24Parser.More_expressions_listContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#prefix_operator}.
	 * @param ctx the parse tree
	 */
	void enterPrefix_operator(Lang24Parser.Prefix_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#prefix_operator}.
	 * @param ctx the parse tree
	 */
	void exitPrefix_operator(Lang24Parser.Prefix_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#multiplicative_operator}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicative_operator(Lang24Parser.Multiplicative_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#multiplicative_operator}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicative_operator(Lang24Parser.Multiplicative_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#additive_operator}.
	 * @param ctx the parse tree
	 */
	void enterAdditive_operator(Lang24Parser.Additive_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#additive_operator}.
	 * @param ctx the parse tree
	 */
	void exitAdditive_operator(Lang24Parser.Additive_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#relational_operator}.
	 * @param ctx the parse tree
	 */
	void enterRelational_operator(Lang24Parser.Relational_operatorContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#relational_operator}.
	 * @param ctx the parse tree
	 */
	void exitRelational_operator(Lang24Parser.Relational_operatorContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(Lang24Parser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(Lang24Parser.ExpressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#multiplicative_expression}.
	 * @param ctx the parse tree
	 */
	void enterMultiplicative_expression(Lang24Parser.Multiplicative_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#multiplicative_expression}.
	 * @param ctx the parse tree
	 */
	void exitMultiplicative_expression(Lang24Parser.Multiplicative_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#additive_expression}.
	 * @param ctx the parse tree
	 */
	void enterAdditive_expression(Lang24Parser.Additive_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#additive_expression}.
	 * @param ctx the parse tree
	 */
	void exitAdditive_expression(Lang24Parser.Additive_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#relational_expression}.
	 * @param ctx the parse tree
	 */
	void enterRelational_expression(Lang24Parser.Relational_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#relational_expression}.
	 * @param ctx the parse tree
	 */
	void exitRelational_expression(Lang24Parser.Relational_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#conjunction_expression}.
	 * @param ctx the parse tree
	 */
	void enterConjunction_expression(Lang24Parser.Conjunction_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#conjunction_expression}.
	 * @param ctx the parse tree
	 */
	void exitConjunction_expression(Lang24Parser.Conjunction_expressionContext ctx);
	/**
	 * Enter a parse tree produced by {@link Lang24Parser#disjunction_expression}.
	 * @param ctx the parse tree
	 */
	void enterDisjunction_expression(Lang24Parser.Disjunction_expressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link Lang24Parser#disjunction_expression}.
	 * @param ctx the parse tree
	 */
	void exitDisjunction_expression(Lang24Parser.Disjunction_expressionContext ctx);
}