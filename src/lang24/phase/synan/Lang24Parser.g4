parser grammar Lang24Parser;

@header {
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

}

@members {

	private Location loc(Token tok) { return new Location((LocLogToken)tok); }
	private Location loc(Token     tok1, Token     tok2) { return new Location((LocLogToken)tok1, (LocLogToken)tok2); }
	private Location loc(Token     tok1, Locatable loc2) { return new Location((LocLogToken)tok1, loc2); }
	private Location loc(Locatable loc1, Token     tok2) { return new Location(loc1, (LocLogToken)tok2); }
	private Location loc(Locatable loc1, Locatable loc2) { return new Location(loc1, loc2); }

}

options{
    tokenVocab=Lang24Lexer;
}

// program
source returns [AstNode ast] : definitions EOF {
    $ast = $definitions.ast;
} ;

// definitions
// −→  (type-definition | variable-definition | function-definition )+
definitions returns [AstNodes ast]: (type_definition | variable_definition | function_definition )+ {
    var list = new LinkedList<AstNode>();
    for (var child : $ctx.children) {
        if (child instanceof Type_definitionContext tchild) {
            list.add(tchild.ast);
        } else if (child instanceof Variable_definitionContext vchild) {
            list.add(vchild.ast);
        } else if (child instanceof Function_definitionContext fchild) {
            list.add(fchild.ast);
        }
    }
    $ast = new AstNodes(list);
};

// type-definition
// −→ identifier = type
type_definition returns [AstNode ast] : IDENTIFIER ASSIGN type {
    $ast = new AstTypDefn((LocLogToken) getCurrentToken(), $IDENTIFIER.getText(), $type.ast);
};

// variable-definition
// −→ identifier : type
variable_definition returns [AstNode ast] : IDENTIFIER COLON type {
    $ast = new AstVarDefn((LocLogToken) getCurrentToken(), $IDENTIFIER.getText(), $type.ast);
};

// function-definition
// −→ identifier (  (parameters)? ) : type  (= statement ( { definitions } )? )?
function_definition returns [AstNode ast]
    : IDENTIFIER LPAREN (parameters)? RPAREN COLON type ( ASSIGN statement ( LBRACE definitions RBRACE )? )? {
        var params = $ctx.parameters != null ? $parameters.ast : new AstNodes();
        var defs = $ctx.definitions != null ? $definitions.ast : new AstNodes();
        var statement = $ctx.statement != null ? $statement.ast : null;
        $ast = new AstFunDefn((LocLogToken) getCurrentToken(), $IDENTIFIER.getText(), params, $type.ast, statement, defs);
} ;

// parameters
// −→  ( ^ )? identifier : type  (,  ( ^ )? identifier : type )∗
parameter returns [AstNode ast]
    : ( crt=CARET )? IDENTIFIER COLON type {
        var location = (LocLogToken) getCurrentToken();
        var name = $IDENTIFIER.getText();
        var type = $type.ast;
        if ($ctx.crt != null) {
            // Pointer
            $ast = new AstFunDefn.AstRefParDefn(location, name, type);
        } else {
            $ast = new AstFunDefn.AstValParDefn(location, name, type);
        }
    } ;

// Tole sicer ni LL(1), ampak bo antlr poskrbel
parameters returns [AstNodes ast]
    : parameters_list {
        $ast = new AstNodes($parameters_list.astList);
    } ;

parameters_list returns [LinkedList<AstNode> astList]
    : parameter {
        $astList = new LinkedList<AstNode>();
        $astList.addFirst($parameter.ast);
    }
    | parameter ( COMMA pl=parameters_list ) {
        $astList = $pl.astList;
        $astList.addFirst($parameter.ast);
    } ;

// statement
// −→ expression ;
// −→ expression = expression ;
// −→ if expression then statement ( else statement )?
// −→ while expression : statement
// −→ return expression ;
// −→ {  (statement )+ }
statement returns [AstStmt ast]
    : expression SEMICOLON {
        $ast = new AstExprStmt((LocLogToken) getCurrentToken(), $expression.ast);
    }
    | expr1=expression ASSIGN expr2=expression SEMICOLON {
        $ast = new AstAssignStmt((LocLogToken) getCurrentToken(), $expr1.ast, $expr2.ast);
    }
    | IF expression THEN st1=statement ( ELSE st2=statement )? {
        var st2 = $ctx.st2 != null ? $st2.ast : null;
        $ast = new AstIfStmt((LocLogToken) getCurrentToken(), $expression.ast, $st1.ast, st2);
    }
    | WHILE expression COLON stmt=statement {
        $ast = new AstWhileStmt((LocLogToken) getCurrentToken(), $expression.ast, $stmt.ast);
    }
    | RETURN expression SEMICOLON {
        $ast = new AstReturnStmt((LocLogToken) getCurrentToken(), $expression.ast);
    }
    | LBRACE (stmt_list+=statement)+ RBRACE {
        $ast = new AstBlockStmt((LocLogToken) getCurrentToken(), $stmt_list.stream().map(x -> x.ast).collect(Collectors.toList()));
    } ;

// type
// −→ void bool char int
// −→ [ intconst ] identifier
// −→ ^ type
// −→ ( components )
// −→ { components }
// −→ identifier
type returns [AstType ast]
    : VOID {
        $ast = new AstAtomType((LocLogToken) getCurrentToken(), AstAtomType.Type.VOID);
    }
    | BOOL {
        $ast = new AstAtomType((LocLogToken) getCurrentToken(), AstAtomType.Type.BOOL);
    }
    | CHAR {
        $ast = new AstAtomType((LocLogToken) getCurrentToken(), AstAtomType.Type.CHAR);
    }
    | INT {
        $ast = new AstAtomType((LocLogToken) getCurrentToken(), AstAtomType.Type.INT);
    }
    | LBRACKET intconst RBRACKET tp=type {
        $ast = new AstArrType((LocLogToken) getCurrentToken(), $tp.ast, $intconst.ast);
    }
    | CARET tp=type {
        $ast = new AstPtrType((LocLogToken) getCurrentToken(), $tp.ast);
    }
    | LPAREN components RPAREN {
        $ast = new AstUniType((LocLogToken) getCurrentToken(), $components.ast);
    }
    | LBRACE components RBRACE {
        $ast = new AstStrType((LocLogToken) getCurrentToken(), $components.ast);
    }
    | IDENTIFIER {
        $ast = new AstNameType((LocLogToken) getCurrentToken(), $IDENTIFIER.getText());
    };


// components
// −→ identifier : type ( , identifier : type )∗
components returns [AstNodes ast]
    : components_list {
        $ast = new AstNodes($components_list.astList);
    } ;

components_list returns [LinkedList<AstNode> astList]
    : single_component {
        $astList = new LinkedList<AstNode>();
        $astList.addFirst($single_component.ast);
    }
    | single_component ( COMMA components_list ) {
        $astList = $components_list.astList;
        $astList.addFirst($single_component.ast);
    } ;

single_component returns [AstRecType.AstCmpDefn ast]
    : IDENTIFIER COLON type {
        $ast = new AstRecType.AstCmpDefn((LocLogToken) getCurrentToken(), $IDENTIFIER.getText(), $type.ast);
    } ;


// expression
// −→ voidconst | boolconst | charconst | intconst | strconst | ptrconst
// −→ identifier ( ( ( expression ( , expression )∗ )? ) )?
// −→ prefix-operator expression
// −→ expression postfix-operator
// −→ expression binary-operator expression
// −→ < type > expression
// −→ expression [ expression ]
// −→ expression . identifier
// −→ sizeof ( expression )
// −→ ( expression )

intconst returns [AstAtomExpr ast] : NUM_LIT {
    $ast = new AstAtomExpr((LocLogToken) getCurrentToken(), AstAtomExpr.Type.INT, $NUM_LIT.getText());
} ;

strconst returns [AstAtomExpr ast] : STR_LIT {
    $ast = new AstAtomExpr((LocLogToken) getCurrentToken(), AstAtomExpr.Type.STR, $STR_LIT.getText());
} ;

charconst returns [AstAtomExpr ast] : CHAR_LIT {
    $ast = new AstAtomExpr((LocLogToken) getCurrentToken(), AstAtomExpr.Type.CHAR, $CHAR_LIT.getText());
} ;

boolconst returns [AstAtomExpr ast] : bl=(TRUE | FALSE) {
    $ast = new AstAtomExpr((LocLogToken) getCurrentToken(), AstAtomExpr.Type.BOOL, $bl.getText());
} ;

voidconst returns [AstAtomExpr ast] : NONE {
    $ast = new AstAtomExpr((LocLogToken) getCurrentToken(), AstAtomExpr.Type.VOID, $NONE.getText());
} ;

ptrconst returns [AstAtomExpr ast] : NIL {
    $ast = new AstAtomExpr((LocLogToken) getCurrentToken(), AstAtomExpr.Type.PTR, $NIL.getText());
} ;

atom returns [AstExpr ast]
    : LPAREN expression RPAREN {
        $ast = $expression.ast;
    }
    | voidconst {
        $ast = $voidconst.ast;
    }
    | boolconst {
        $ast = $boolconst.ast;
    }
    | charconst {
        $ast = $charconst.ast;
    }
    | intconst {
        $ast = $intconst.ast;
    }
    | strconst {
        $ast = $strconst.ast;
    }
    | ptrconst {
        $ast = $ptrconst.ast;
    }
    | IDENTIFIER ( LPAREN ( more_expressions )? RPAREN )? {
        if ($LPAREN != null) {
            var exprs = $ctx.more_expressions != null ? $more_expressions.ast : new AstNodes();
            $ast = new AstCallExpr((LocLogToken) getCurrentToken(), $IDENTIFIER.getText(), exprs);
        } else {
            $ast = new AstNameExpr((LocLogToken) getCurrentToken(), $IDENTIFIER.getText());
        }
    }
    | atm=atom DOT IDENTIFIER {
        $ast = new AstCmpExpr((LocLogToken) getCurrentToken(), $atm.ast, $IDENTIFIER.getText());
    }
    | atm=atom LBRACKET expression RBRACKET {
        $ast = new AstArrExpr((LocLogToken) getCurrentToken(), $atm.ast, $expression.ast);
    }
    | atm=atom CARET {
        $ast = new AstSfxExpr((LocLogToken) getCurrentToken(), AstSfxExpr.Oper.PTR, $atm.ast);
    }
    | prefix_operator atm=atom {
        $ast = new AstPfxExpr((LocLogToken) getCurrentToken(), $prefix_operator.op, $atm.ast);
    }
    | LT type GT expression {
        $ast = new AstCastExpr((LocLogToken) getCurrentToken(), $type.ast, $expression.ast);
    }
    | SIZEOF LPAREN type RPAREN {
        $ast = new AstSizeofExpr((LocLogToken) getCurrentToken(), $type.ast);
    } ;

more_expressions returns [AstNodes ast]
    : more_expressions_list {
        $ast = new AstNodes($more_expressions_list.astList);
    } ;

more_expressions_list returns [LinkedList<AstNode> astList]
    : expression {
        $astList = new LinkedList<AstNode>();
        $astList.addFirst($expression.ast);
    }
    | expression ( COMMA more_expressions_list ) {
        $astList = $more_expressions_list.astList;
        $astList.addFirst($expression.ast);

    } ;

prefix_operator returns [AstPfxExpr.Oper op]
    : NOT {
        $op = AstPfxExpr.Oper.NOT;
    }
    | PLUS {
        $op = AstPfxExpr.Oper.ADD;
    }
    | MINUS {
        $op = AstPfxExpr.Oper.SUB;
    }
    | CARET {
        $op = AstPfxExpr.Oper.PTR;
    } ;

multiplicative_operator returns [AstBinExpr.Oper op]
    : MUL {
        $op = AstBinExpr.Oper.MUL;
    }
    | DIV {
        $op = AstBinExpr.Oper.DIV;
    }
    | MOD {
        $op = AstBinExpr.Oper.MOD;
    } ;

additive_operator returns [AstBinExpr.Oper op]
    : PLUS {
        $op = AstBinExpr.Oper.ADD;
    }
    | MINUS {
        $op = AstBinExpr.Oper.SUB;
    } ;

relational_operator returns [AstBinExpr.Oper op]
    : EQ {
        $op = AstBinExpr.Oper.EQU;
    }
    | NE {
        $op = AstBinExpr.Oper.NEQ;
    }
    | LT {
        $op = AstBinExpr.Oper.LTH;
    }
    | GT {
        $op = AstBinExpr.Oper.GTH;
    }
    | LE {
        $op = AstBinExpr.Oper.LEQ;
    }
    | GE {
        $op = AstBinExpr.Oper.GEQ;
    } ;

expression returns [AstExpr ast]
    : multiplicative_expression {
        $ast = $multiplicative_expression.ast;
    }
    | additive_expression {
        $ast = $additive_expression.ast;
    }
    | relational_expression {
        $ast = $relational_expression.ast;
    }
    | conjunction_expression {
        $ast = $conjunction_expression.ast;
    }
    | disjunction_expression {
        $ast = $disjunction_expression.ast;
    } ;


multiplicative_expression returns [AstExpr ast]
    : mul_xpr=multiplicative_expression multiplicative_operator atom {
        $ast = new AstBinExpr((LocLogToken) getCurrentToken(), $multiplicative_operator.op, $mul_xpr.ast, $atom.ast);
    }
    | atom {
        $ast = $atom.ast;
    } ;

additive_expression returns [AstExpr ast]
    : add_xpr=additive_expression additive_operator multiplicative_expression {
        $ast = new AstBinExpr((LocLogToken) getCurrentToken(), $additive_operator.op, $add_xpr.ast, $multiplicative_expression.ast);
    }
    | multiplicative_expression {
        $ast = $multiplicative_expression.ast;
    } ;

relational_expression returns [AstExpr ast]
    : rel_xpr=relational_expression relational_operator additive_expression {
        $ast = new AstBinExpr((LocLogToken) getCurrentToken(), $relational_operator.op, $rel_xpr.ast, $additive_expression.ast);
    }
    | additive_expression {
        $ast = $additive_expression.ast;
    } ;

conjunction_expression returns [AstExpr ast]
    : con_xpr=conjunction_expression AND relational_expression {
        $ast = new AstBinExpr((LocLogToken) getCurrentToken(), AstBinExpr.Oper.AND, $con_xpr.ast, $relational_expression.ast);
    }
    | relational_expression {
        $ast = $relational_expression.ast;
    } ;

disjunction_expression returns [AstExpr ast]
    : dis_xpr=disjunction_expression OR conjunction_expression {
        $ast = new AstBinExpr((LocLogToken) getCurrentToken(), AstBinExpr.Oper.OR, $dis_xpr.ast, $conjunction_expression.ast);
    }
    | conjunction_expression {
        $ast = $conjunction_expression.ast;
    } ;


// Error
/*error : . {
throw new Report.Error(new Location(getCurrentToken().getLine(), getCurrentToken().getCharPositionInLine()),
                        "Syntax error: " + getCurrentToken().getText());
} ;*/