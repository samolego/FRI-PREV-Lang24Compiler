The LANG’24 Language Specification
1 Lexical structure
Programs in the LANG’24 programming language are written in ASCII character set, e.g., no additional
charactersdenotingpost-alveolarconsonantsareallowed.
ProgramsintheLANG’24programminglanguageconsistofthefollowinglexicalelements:
• Literals:
– numericalliterals:
Anonemptyfinitestringofdecimaldigits(0...9)optionallyprecededbyasign(+or-).
– characterliterals:
Acharacterenclosedinsinglequotes(’). Acharacterinastringliteralcanbespecifiedby(a)
anyprintableASCIIcharacter, i.e., withASCIIcodeinrange{32...126}butthesinglequote
andthebackslashmustbeprecededbythebackslash(\), (b)acontrolsequence\ndenoting
the end of line or (c) an ASCII code represented as \XX where X stands for any uppercase
hexadecimaldigit(0...9orA...F).
– stringliterals:
A possibly empty string of characters enclosed in double quotes ("). A character in a string
literal can be specified by (a) any printable ASCII character, i.e., with ASCII code in range
{32...126}butthedoublequoteandthebackslashmustbeprecededbythebackslash(\),(b)a
controlsequence\ndenotingtheendoflineor(c)anASCIIcoderepresentedas\XXwhereX
standsforanyuppercasehexadecimaldigit(0...9orA...F).
• Symbols:
(){}[].,:;==!=<><=>=*/%+-^=
• Identifiers:
Anonemptyfinitestringofletters(A...Zanda...z),decimaldigits(0...9),andunderscores(_)that
(a)startswitheitheraletteroranunderscoreand(b)isnotakeywordoraconstant.
• Built-indatatypes:
• Keywords:
– literals: truefalsenilnone
– built-indatatypes: boolcharintvoid
– operators: andnotorsizeof
– statements: returnifthenelsewhile
• Comments:
Astringofcharactersstartingwithahash(#)andextendingtotheendofline.
• Whitespace:
Space,horizontaltab(HT),linefeed(LF)andcarriagereturn(CR).Linefeedalonedenotestheend
oflinewithinasourcefile. Horizontaltabsareconsideredtobe8spaceswide.
Lexicalelementsarerecognizedfromlefttorightusingthelongestmatchapproach.
1

| 2 Syntax | structure |     |     |     |     |
| -------- | --------- | --- | --- | --- | --- |
The concrete syntax of the LANG’24 programming language is defined by a context free grammar with
thestartsymboldefinitionsandthefollowingproductions:
definitions
| (cid:0) |     |     |     | (cid:1)+ |     |
| ------- | --- | --- | --- | -------- | --- |
−→ type-definition|variable-definition|function-definition
type-definition
−→identifier=type
variable-definition
−→identifier:type
function-definition
|               | (cid:0)    | (cid:1)?):type | (cid:0)    | (cid:0)       | (cid:1)?(cid:1)? |
| ------------- | ---------- | -------------- | ---------- | ------------- | ---------------- |
| −→identifier( | parameters |                | =statement | {definitions} |                  |
parameters
| (cid:0) (cid:1)?identifier:type |     | (cid:0) (cid:0) | (cid:1)?identifier:type | (cid:1)∗ |     |
| ------------------------------- | --- | --------------- | ----------------------- | -------- | --- |
| −→ ^                            |     | , ^             |                         |          |     |
statement
−→expression;
−→expression=expression;
|                             |     |     | (cid:0)       | (cid:1)? |     |
| --------------------------- | --- | --- | ------------- | -------- | --- |
| −→ifexpressionthenstatement |     |     | elseststement |          |     |
−→whileexpression:statement
−→returnexpression;
| (cid:0) | (cid:1)+} |     |     |     |     |
| ------- | --------- | --- | --- | --- | --- |
−→{ statement
type
|     | (cid:12) (cid:12) | (cid:12) |     |     |     |
| --- | ----------------- | -------- | --- | --- | --- |
−→void(cid:12)bool(cid:12)char(cid:12)int
−→[intconst]type
−→^type
−→(components)
−→{components}
−→identifier
components
|                   | (cid:0) |                  | (cid:1)∗ |     |     |
| ----------------- | ------- | ---------------- | -------- | --- | --- |
| −→identifier:type |         | ,identifier:type |          |     |     |
expression
|     | (cid:12) | (cid:12) | (cid:12) (cid:12) | (cid:12) |     |
| --- | -------- | -------- | ----------------- | -------- | --- |
−→voidconst(cid:12)boolconst(cid:12)charconst(cid:12)intconst(cid:12)strconst(cid:12)ptrconst
| −→identifier | (cid:0) ( (cid:0) expression | (cid:0) ,expression | (cid:1)∗(cid:1)?) | (cid:1)? |     |
| ------------ | ---------------------------- | ------------------- | ----------------- | -------- | --- |
−→prefix-operatorexpression
−→expressionbinary-operatorexpression
−→<type>expression
−→expression[expression]
−→expression.identifier
−→expression^
−→sizeof(type)
−→(expression)
2

Symbols voidconst, boolconst, charconst, intconst, strconst, and ptrconst denote void constant none,
booleanconstantstrueandfalse, characterliterals, integerliterals, stringliterals, andpointerconstant
nil,respectively.
Theprecedenceoftheoperatorsisasfollows:
| postfixoperators        | [·]^.      | THEHIGHESTPRECEDENCE |
| ----------------------- | ---------- | -------------------- |
| prefixoperators         | not+-^<·>  |                      |
| multiplicativeoperators | */%        |                      |
| additiveoperators       | +-         |                      |
| relationaloperators     | ==!=<><=>= |                      |
| conjunctiveoperator     | and        |                      |
| disjunctiveoperator     | or         | THELOWESTPRECEDENCE  |
Binaryoperatorsareleftassociative.
Theelsepartoftheconditionalstatementbindstothenearestprecedingifpart.
3 Semantic structure
3.1 Namebinding
Let function [[·]] BIND bind a name to its declaration according to the rules of namespaces and scopes de-
scribedbelow. Hence,thevalueoffunction[[·]] dependsonthecontextofitsargument.
BIND
Namespaces. Therearetwokindsofanamespaces:
1. Namesoftypes,functions,variablesandparametersbelongtoonesingleglobalnamespace.
2. Namesofcomponentsbelongtostructure-orunion-specificnamespaces,i.e.,eachstructureorunion
definesitsownnamespacecontainingnamesofitscomponents.
Scopes. Twonewscopesarecreatedineveryfunctiondefinition
identifier(parameters):type=statement{definitions}
asfollows:
1. The name, the parameter types and the result type belong to the scope in which the function is
defined.
2. Theparameternamesbelongtothescopenestedwithinthescopeinwhichthefunctionisdefined.
3. Statements and definitions belong to the scope nested within the scope in which parameter names
aredefined.
Iftherearenoparameters,statementsordefinitions,thescopesarecreatednevertheless.
Allnamesdeclaredwithinagivenscopearevisibleintheentirescopeunlesshiddenbyadefinitioninthe
nestedinnerscope. Anamecanbedeclaredwithinthesamescopeatmostonce.
3

3.2 Typesystem
Theset
T ={void,char,int,bool} (atomictypes)
d
∪{arr(n×τ)|n>0∧τ ∈T } (arrays)
d
∪{struct (τ ,...,τ )|n>0∧τ ,...,τ ∈T } (structs)
id1,...,idn 1 n 1 n d
∪{union (τ ,...,τ )|n>0∧τ ,...,τ ∈T } (unions)
id1,...,idn 1 n 1 n d
∪{ptr(τ)|τ ∈T } (pointers)
d
denotesthesetofalldatatypesofLANG’24. Theset
T =T (datatypes)
d
∪{(τ ,...,τ )→τ |n≥0∧τ ,...,τ ,τ ∈T } (functions)
1 n 1 n d
denotesthesetofalltypesofLANG’24.
Structural equivalence of types: Types τ and τ are equivalent if (a) τ = τ or (b) if they are type
1 2 1 2
synonyms(introducedbychainsoftypedeclarations)oftypesτ′ andτ′ whereτ′ =τ′.
1 2 1 2
Semanticfunctions
[[·]] :P →T and [[·]] :P →T
ISTYPE OFTYPE
mapsyntacticphrasesofLANG’24totypes. Function[[·]] denotesthetypedescribedbyaphrase,
ISTYPE
function[[·]] denotesthetypeofavaluedescribedbyaphrase.
OFTYPE
Thefollowingassumptionsaremadeintherulesbelow:
• Functionvalmapslexemestodataofthespecifiedtype.
• τ ∈T unlessspecifiedotherwise.
d
Typeexpressions.
(T1)
[[void]] =void [[bool]] =bool [[char]] =char [[int]] =int
ISTYPE ISTYPE ISTYPE ISTYPE
[[type]] =τ val(int)=n
ISTYPE
0<n≤263−1 τ ∈T \{void}
d
(T2)
[[[int]type]] =arr(n×τ)
ISTYPE
n>0 ∀i∈{1...n}:[[type ]] =τ τ ∈T \{void}
i ISTYPE i i d (T3)
[[(id :type ,...,id :type )]] =struct (τ ,...,τ )
1 1 n n ISTYPE id1,...,idn 1 n
n>0 ∀i∈{1...n}:[[type ]] =τ τ ∈T \{void}
i ISTYPE i i d (T4)
[[{id :type ,...,id :type }]] =union (τ ,...,τ )
1 1 n n ISTYPE id1,...,idn 1 n
[[type]] =τ τ ∈T
ISTYPE d
(T5)
[[^type]] =ptr(τ)
ISTYPE
Valueexpressions.
(V1)
[[none]] =void [[nil]] =ptr(void) [[string]] =ptr(char)
OFTYPE OFTYPE OFTYPE
(V2)
[[bool]] =bool [[char]] =char [[int]] =int
OFTYPE OFTYPE OFTYPE
4

[[expr]] =bool [[expr]] =int op ∈{+,-}
OFTYPE OFTYPE
(V3)
[[notexpr]] =bool [[opexpr]] =int
OFTYPE OFTYPE
[[expr ]] =bool [[expr ]] =bool op ∈{and,or}
1 OFTYPE 2 OFTYPE (V4)
[[expr opexpr ]] =bool
1 2 OFTYPE
[[expr ]] =int [[expr ]] =int op ∈{+,-,*,/,%}
1 OFTYPE 2 OFTYPE (V5)
[[expr opexpr ]] =int
1 2 OFTYPE
[[expr ]] =τ [[expr ]] =τ
1 OFTYPE 2 OFTYPE
τ ∈{bool,char,int}∪{ptr(τ)|τ ∈T } op ∈{==,!=}
d
(V6)
[[expr opexpr ]] =bool
1 2 OFTYPE
[[expr ]] =τ [[expr ]] =τ
1 OFTYPE 2 OFTYPE
τ ∈{char,int}∪{ptr(τ)|τ ∈T } op ∈{<=,>=,<,>}
d
(V7)
[[expr opexpr ]] =bool
1 2 OFTYPE
[[expr]] =τ [[expr]] =true [[expr]] =ptr(τ)
OFTYPE ISLVAL OFTYPE
(V8)
[[^expr]] =ptr(τ) [[expr^]] =τ
OFTYPE OFTYPE
[[expr ]] =arr(n×τ) [[expr ]] =int [[expr ]] =true
1 OFTYPE 2 OFTYPE 1 ISLVAL (V9)
[[expr [expr ]]] =τ
1 2 OFTYPE
[[expr]] =struct (τ ,...,τ ) identifier =id
OFTYPE id1,...,idn 1 n i
(V10)
[[expr.identifier]] =τ
OFTYPE i
[[expr]] =union (τ ,...,τ ) identifier =id
OFTYPE id1,...,idn 1 n i
(V11)
[[expr.identifier]] =τ
OFTYPE i
[[identifier]] =(τ ,...,τ )→τ
OFTYPE 1 n
∀i∈{1...n}:[[expr ]] =τ ∧τ ∈{bool,char,int}∪{ptr(τ)|τ ∈T }
i OFTYPE i i d
τ ∈{void,bool,char,int}∪{ptr(τ)|τ ∈T }
d
∀i∈{1...n}:(thei-thparameterisacall-by-reference)=⇒[[expr ]] =true
i ISLVAL (V12)
[[identifier(expr ,...,expr )]] =τ
1 n OFTYPE
[[type]] =τ [[expr]] =τ τ ,τ ∈{char,int}∪{ptr(τ)|τ ∈T }
ISTYPE 1 OFTYPE 2 1 2 d
(V13)
[[<type>expr)]] =τ
OFTYPE 1
[[expr]] =τ [[type]] =τ
OFTYPE ISTYPE
(V14)
[[(expr)]] =τ [[sizeof(type)]] =int
OFTYPE OFTYPE
Statements.
[[expr ]] =τ [[expr ]] =τ
1 OFTYPE 2 OFTYPE
τ ∈{bool,char,int}∪{ptr(τ)|τ ∈T }
d
[[expr ]] =true
1 ISLVAL (S1)
[[expr =expr ;]] =void
1 2 OFTYPE
5

[[expr]] =void
OFTYPE
(S2)
[[expr;]] =void
OFTYPE
[[expr]] =bool [[stmts]] =τ
OFTYPE OFTYPE
(S3)
[[ifexpr thenstmts]] =void
OFTYPE
[[expr]] =bool [[stmts ]] =τ [[stmts ]] =τ
OFTYPE 1 OFTYPE 1 2 OFTYPE 2
(S4)
[[ifexpr thenstmts elsestmts ]] =void
1 2 OFTYPE
[[expr]] =bool [[stmts]] =τ
OFTYPE OFTYPE
(S5)
[[whileexpr :stmts]] =void
OFTYPE
[[expr]] =τ (theresulttypeoftheinnermostfunctionisτ)
OFTYPE
(S6)
[[returnexpr;]] =void
OFTYPE
n>0 ∀i∈{1...n}:[[stmt ]] =τ
i OFTYPE i
(S7)
[[{stmt ...stmt }]] =void
1 n OFTYPE
Declarations.
[[identifier]] =identifier=type [[type]] =τ
BIND ISTYPE
(D1)
[[identifier]] =τ
ISTYPE
[[identifier]] =identifier:type [[type]] =τ τ ∈T \{void}
BIND ISTYPE d
(D2)
[[identifier]] =τ
OFTYPE
[[identifier]] =identifier(identifer :type ,...,identifer :type ):type
BIND 1 1 n n
∀i∈{1...n}:[[type ]] =τ ∧τ ∈{bool,char,int}∪{ptr(τ)|τ ∈T }
i ISTYPE i i d
[[type]] =τ τ ∈{void,bool,char,int}∪{ptr(τ)|τ ∈T }
ISTYPE d
(D3)
[[identifier]] =(τ ,...,τ )→τ
OFTYPE 1 n
[[identifier]] =identifier(identifer :type ,...,identifer :type ):type=stmt
BIND 1 1 n n
∀i∈{1...n}:[[type ]] =τ ∧τ ∈{bool,char,int}∪{ptr(τ)|τ ∈T }
i ISTYPE i i d
[[type]] =τ [[stmt]] =void τ ∈{void,bool,char,int}∪{ptr(τ)|τ ∈T }
ISTYPE OFTYPE d
[[identifier]] =(τ ,...,τ )→τ
OFTYPE 1 n
(D4)
3.3 Lvalues
Thesemanticfunction
[[·]] :P →{true,false}
ISLVAL
denoteswhichphrasesrepresentlvalues.
[[identifier]] =variabledeclaration [[identifier]] =parameterdeclaration
BIND BIND
[[identifier]] =true [[identifier]] =true
ISLVAL ISLVAL
6

[[expr]] =ptr(τ) [[expr]] =true [[expr]] =true
OFTYPE ISLVAL ISLVAL
[[expr^]] =true [[expr[expr′]]] =true [[expr.identifier]] =true
ISLVAL ISLVAL ISLVAL
Inallothercasesthevalueof[[·]] equalsfalse.
ISLVAL
3.4 Linkage
Avariableorafunctionhasexternallinkageifitisnotdeclaredinsideafunction.
3.5 Operationalsemantics
Operationalsemanticsisdescribedbysemanticfunctions
[[·]] : P ×M→I×M
ADDR
[[·]] : P ×M→I×M
EXPR
[[·]] : P ×M→M
STMT
wherePdenotesthesetofphrasesofPREV’23,Idenotesthesetof64-bitintegers,andMdenotespossible
statesofthememory. Unaryoperatorsandbinaryoperatorsperform64-bitsignedoperations(exceptfor
typecharwhereoperationsareperformedonthelower8bitsonly).
Auxilary function addr returns either an absolute address for a static variable or a string constant or an
offsetforalocalvariable, parameterorrecordcomponent. Auxilaryfunctionsizeof returnsthesizeofa
type. AuxilaryfunctionvalreturnsthevalueofanintegerconstantoranASCIIcodeofacharconstant.
Addresses.
(A1)
[[string]]M =⟨addr(string),M⟩
ADDR
addr(identifier)=a
(A2)
[[identifier]]M =⟨a,M⟩
ADDR
[[expr ]]M =⟨n ,M′⟩ [[expr ]]M′ =⟨n ,M′′⟩ [[expr ]] =arr(n×τ)
1 ADDR 1 2 EXPR 2 1 OFTYPE (A3)
[[expr [expr ]]]M =⟨n +n ∗sizeof(τ),M′′⟩
1 2 ADDR 1 2
[[expr]]M =⟨n ,M′⟩
ADDR 1 (A4)
[[expr.identifier]]M =⟨n +addr(identifier),M′⟩
ADDR 1
[[expr]]M =⟨n,M′⟩
EXPR (A5)
[[expr^]]M =⟨n,M′⟩
ADDR
Expressions.
(EX1)
[[none]]M =⟨undef,M⟩ [[nil]]M =⟨0,M⟩
EXPR EXPR
(EX2)
[[true]]M =⟨1,M⟩ [[false]]M =⟨0,M⟩
EXPR EXPR
7

(EX3)
[[char]]M =⟨val(char),M⟩ [[int]]M =⟨val(int),M⟩
EXPR EXPR
[[expr]]M =⟨n,M′⟩ op∈{not,+,-}
EXPR (EX4)
[[opexpr]]M =⟨opn,M′⟩
EXPR
[[expr ]]M =⟨n ,M′⟩ [[expr ]]M′ =⟨n ,M′′⟩ op∈{or,and,==,!=,<,>,<=,>=,+,-,*,/,%}
1 EXPR 1 2 EXPR 2
[[expr opexpr ]]M =⟨n opn ,M′′⟩
1 2 EXPR 1 2
(EX5)
[[expr]]M =⟨n,M′⟩ [[expr]]M =⟨n,M′⟩
ADDR EXPR (EX6)
[[^expr]]M =⟨n,M′⟩ [[expr^]]M =⟨M′[n],M′⟩
EXPR EXPR
addr(identifier)=a
(EX7)
[[identifier]]M =⟨M[a],M⟩
EXPR
[[expr [expr ]]]M =⟨a,M′⟩
1 2 ADDR (EX8)
[[expr [expr ]]]M =⟨M′[a],M′⟩
1 2 EXPR
[[expr.identifier]]M =⟨a,M′⟩
ADDR (EX9)
[[expr.identifier]]M =⟨M′[a],M′⟩
EXPR
[[expr ]]M0 =⟨n ,M ⟩ ... [[expr ]]Mm−1 =⟨n ,M ⟩
1 EXPR 1 1 m EXPR m m (EX10)
[[identifier(expr ,...,expr )]]M0 =⟨identifier(n ,...,n ),M ⟩
1 m EXPR 1 m m
[[expr]]M =⟨n,M′⟩
EXPR (EX11)
[[(expr)]]M =⟨n,M′⟩
EXPR
[[expr]]M =⟨n,M′⟩ [[type]] ̸=char
EXPR ISTYPE (EX12)
[[<type>expr]]M =⟨n,M′⟩
EXPR
[[expr]]M =⟨n,M′⟩ [[type]] =char
EXPR ISTYPE (EX13)
[[<type>expr]]M =⟨nmod256,M′⟩
EXPR
Statements.
[[expr]]M =⟨n,M′⟩
EXPR (ST1)
[[expr]]M =M′
STMT
[[expr ]]M =⟨n ,M′⟩ [[expr ]]M′ =⟨n ,M′′⟩
1 ADDR 1 (cid:26) 2 EXPR 2
n a=n
∀a:M′′′[a]= 2 1
M′′[a] otherwise
(ST2)
[[expr =expr ]]M =M′′′
1 2 STMT
[[expr]]M =⟨true,M′⟩ [[stmt ]]M′ =M′′
EXPR 1 STMT (ST3)
[[ifexpr thenstmt ]] =M′′
1 STMT
8

|     |     |     | [[expr]]M |     | =⟨false,M′⟩ |     |     |     |
| --- | --- | --- | --------- | --- | ----------- | --- | --- | --- |
EXPR
(ST4)
|     |           | [[ifexpr |            | thenstmt | ]]     | =M′    |      |     |
| --- | --------- | -------- | ---------- | -------- | ------ | ------ | ---- | --- |
|     |           |          |            |          | 1 STMT |        |      |     |
|     | [[expr]]M |          | =⟨true,M′⟩ |          | [[stmt | ]]M′   | =M′′ |     |
|     |           | EXPR     |            |          |        | 1 STMT |      |     |
(ST5)
|     | [[ifexpr |     | thenstmt | elsestmt |     | ]]     | =M′′ |     |
| --- | -------- | --- | -------- | -------- | --- | ------ | ---- | --- |
|     |          |     |          | 1        |     | 2 STMT |      |     |
]]M′
|     | [[expr]]M |      | =⟨false,M′⟩ |     | [[stmt |        | =M′′ |       |
| --- | --------- | ---- | ----------- | --- | ------ | ------ | ---- | ----- |
|     |           | EXPR |             |     |        | 2 STMT |      | (ST6) |
=M′′
|     | [[ifexpr  |      | thenstmt   | 1 elsestmt |            | 2 ]] STMT |      |     |
| --- | --------- | ---- | ---------- | ---------- | ---------- | --------- | ---- | --- |
|     | [[expr]]M |      | =⟨true,M′⟩ |            | [[stmt]]M′ |           | =M′′ |     |
|     |           | EXPR |            |            |            | STMT      |      |     |
(ST7)
|     | [[whileexpr |     | :stmt]]M  | =[[whileexpr |             |     | :stmt]]M′′ |     |
| --- | ----------- | --- | --------- | ------------ | ----------- | --- | ---------- | --- |
|     |             |     |           | STMT         |             |     | STMT       |     |
|     |             |     | [[expr]]M |              | =⟨false,M′⟩ |     |            |     |
EXPR
(ST8)
|     |     | [[whileexpr |     | :stmt]]M |     | =M′ |     |     |
| --- | --- | ----------- | --- | -------- | --- | --- | --- | --- |
STMT
|     |        | ]]M0 |      |        |        | ]]Mm−1 |     |       |
| --- | ------ | ---- | ---- | ------ | ------ | ------ | --- | ----- |
|     | [[stmt |      |      | =M ... | [[stmt |        | =M  |       |
|     |        | 1    | STMT | 1      |        | m STMT | m   | (ST9) |
}]]M0
|              |     | [[{stmt |           | ... stmt                          |         | =M  |     |        |
| ------------ | --- | ------- | --------- | --------------------------------- | ------- | --- | --- | ------ |
|              |     |         | 1         |                                   | m STMT  |     | m   |        |
|              |     |         | [[expr]]M |                                   | =⟨n,M′⟩ |     |     |        |
|              |     |         |           | EXPR                              |         |     |     | (ST10) |
| [[returnexpr |     | ]]M     |           | =M′ ∧functionterminatesreturningn |         |     |     |        |
STMT
9
