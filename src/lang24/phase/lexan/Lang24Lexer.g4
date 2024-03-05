lexer grammar Lang24Lexer;

@header {
package lang24.phase.lexan;

import lang24.common.report.*;
import lang24.data.token.*;
}

@members {
@Override
public LocLogToken nextToken() {
    return (LocLogToken) super.nextToken();
}
}

// Literals:
// numerical literals:
// A nonempty finite string of decimal digits (0... 9) optionally preceded by a sign (+ or -).
INTCONST : DEC_DIGIT+ ;
DEC_DIGIT : [0-9] ;
HEX_DIGIT : DEC_DIGIT | [A-F] ;

// character literals
// A character enclosed in single quotes (').
// A character in a string literal can be specified by:
// - any printable ASCII character, i.e., with ASCII code in range {32...126} but the single quote and the backslash must be preceded
//   by the backslash (\),
// - a control sequence \n denoting the end of line
// - an ASCII code represented as \XX where X stands for any uppercase
//   hexadecimal digit (0... 9 or A... F).

// CHARACTER - ascii 32-126 (space - tilde) minus both quotes and backslash
fragment PRINTABLE_ASCII : [ -!#-&(-[\]-~] ;
fragment ESC_SEQ
	: '\\'
	(
		'n'
		| HEX_DIGIT HEX_DIGIT
		| '\\'
	);

CHARCONST
	: '\'' (
		ESC_SEQ
		| PRINTABLE_ASCII
		| '\\\''
		| '"'
	)
	'\'' ;

STRCONST
	: '"' (
		ESC_SEQ
		| PRINTABLE_ASCII
		| '\\"'
		| '\''
	)*
	'"' ;

// Keywords
// and bool char else if int nil none not or sizeof then return void while
AND : 'and' ;
BOOL : 'bool' ;
CHAR : 'char' ;
ELSE : 'else' ;
FALSE : 'false' ;
IF : 'if' ;
INT : 'int' ;
NIL : 'nil' ;
NONE : 'none' ;
NOT : 'not' ;
OR : 'or' ;
SIZEOF : 'sizeof' ;
THEN : 'then' ;
RETURN : 'return' ;
TRUE : 'true' ;
VOID : 'void' ;
WHILE : 'while' ;

// White space:
// Space, horizontal tab (HT), line feed (LF) and carriage return (CR).
// Line feed alone denotes the end of line within a source file.
// Horizontal tabs are considered to be **8** spaces wide.
fragment TAB : '\t' ;
fragment SPACE : ' ' ;
fragment CR : '\r' ;
fragment LF : '\n' ;
WS : (SPACE
	| CR ? LF
	| TAB {setCharPositionInLine(getCharPositionInLine() + 8 - (getCharPositionInLine() % 8));}
	)+ -> skip ;



// Comments:
// A string of characters starting with a hash (`#`) and extending to the end of line.
COMMENT : '#' ~ [\n]* -> skip ;

// Symbols
// ( ) { } [ ] . , : ; == != < > <= >= * / % + - ^ =
LPAREN : '(' ;
RPAREN : ')' ;
LBRACE : '{' ;
RBRACE : '}' ;
LBRACKET : '[' ;
RBRACKET : ']' ;
DOT : '.' ;
COMMA : ',' ;
COLON : ':' ;
SEMICOLON : ';' ;
EQ : '==' ;
NE : '!=' ;
LT : '<' ;
GT : '>' ;
LE : '<=' ;
GE : '>=' ;
MUL : '*' ;
DIV : '/' ;
MOD : '%' ;
PLUS : '+' ;
MINUS : '-' ;
CARET : '^' ;
ASSIGN : '=' ;

// Identifiers
// A nonempty finite string of letters (A. . . Z and a. . . z), decimal digits (0. . . 9), and underscores (_) that
// - starts with either a letter or an underscore and
// - is not a keyword or a constant.
IDENTIFIER : [a-zA-Z_][a-zA-Z_0-9]* ;

// Error
// Any character not recognized by the lexer
ERROR : . {
    throw new Report.Error(new Location(getLine(), getCharPositionInLine()), "Lexing error: " + getText());

case 0xFFFFFFFF:  // Dummy case to make compiler happy
} ;