parser grammar Lang24Parser;

@header {
package lang24.phase.synan;

import java.util.*;
import lang24.common.report.*;
import lang24.data.token.*;
import lang24.phase.lexan.*;

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
source : definitions EOF ;

// definitions
// −→  (type-definition | variable-definition | function-definition )+
definitions : (type_definition | variable_definition | function_definition )+ ;

// type-definition
// −→ identifier = type
type_definition : IDENTIFIER ASSIGN type ;

// variable-definition
// −→ identifier : type
variable_definition : IDENTIFIER COLON type ;

// function-definition
// −→ identifier (  (parameters)? ) : type  (= statement ( { definitions } )? )?
function_definition
    : IDENTIFIER LPAREN (parameters)? RPAREN
        COLON type ( ASSIGN statement ( LBRACE definitions RBRACE )? )? ;

// parameters
// −→  ( ^ )? identifier : type  (,  ( ^ )? identifier : type )∗
id_type : IDENTIFIER COLON type ;
parameter : ( CARET )? id_type ;
// Tole sicer ni LL(1), ampak bo antlr poskrbel
parameters
    : parameter
    | parameter ( COMMA parameters ) ;

// statement
// −→ expression ;
// −→ expression = expression ;
// −→ if expression then statement ( else statement )?
// −→ while expression : statement
// −→ return expression ;
// −→ {  (statement )+ }
statement
    : expression SEMICOLON
    | expression ASSIGN expression SEMICOLON
    | IF expression THEN statement ( ELSE statement )?
    | WHILE expression COLON statement
    | RETURN expression SEMICOLON
    | LBRACE (statement)+ RBRACE ;

// type
// −→ void bool char int
// −→ [ intconst ] identifier
// −→ ^ type
// −→ ( components )
// −→ { components }
// −→ identifier
type
    : VOID | BOOL | CHAR | INT
    | LBRACKET INTCONST RBRACKET type
    | CARET type
    | LPAREN components RPAREN
    | LBRACE components RBRACE
    | IDENTIFIER ;


// components
// −→ identifier : type ( , identifier : type )∗
components
    : id_type
    | id_type ( COMMA components ) ;


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

boolconst : TRUE | FALSE ;
voidconst : NONE ;
ptrconst : NIL ;

atom
    : LPAREN expression RPAREN
    | voidconst
    | boolconst
    | CHARCONST
    | INTCONST
    | STRCONST
    | ptrconst
    | IDENTIFIER ( LPAREN ( more_expressions )? RPAREN )?
    | atom postfix_operator
    | prefix_operator atom
    | atom DOT IDENTIFIER
    | LT type GT expression
    | atom LBRACKET expression RBRACKET
    | SIZEOF LPAREN expression RPAREN ;

more_expressions
    : expression
    | expression ( COMMA more_expressions ) ;

postfix_operator
    : LBRACKET
    | DOT
    | RBRACKET
    | CARET
    | DOT ;

prefix_operator
    : NOT
    | PLUS
    | MINUS
    | CARET
    | LT
    | DOT
    | GT;

multiplicative_operator
    : MUL
    | DIV
    | MOD ;

additive_operator
    : PLUS
    | MINUS ;

relational_operator
    : EQ
    | NE
    | LT
    | GT
    | LE
    | GE ;

expression
    : multiplicative_expression
    | additive_expression
    | relational_expression
    | conjunction_expression
    | disjunction_expression ;

multiplicative_expression
    : multiplicative_expression multiplicative_operator atom
    | atom ;

additive_expression
    : additive_expression additive_operator multiplicative_expression
    | multiplicative_expression ;

relational_expression
    : relational_expression relational_operator additive_expression
    | additive_expression ;

conjunction_expression
    : conjunction_expression AND relational_expression
    | relational_expression ;

disjunction_expression
    : disjunction_expression OR conjunction_expression
    | conjunction_expression ;


// Error
/*error : . {
throw new Report.Error(new Location(getCurrentToken().getLine(), getCurrentToken().getCharPositionInLine()),
                        "Syntax error: " + getCurrentToken().getText());
} ;*/