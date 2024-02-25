# The LANG’24 Language Specification
## 1 Lexical structure
Programs in the LANG’24 programming language are written in ASCII character set, e.g., no additional
characters denoting post-alveolar consonants are allowed.


Programs in the LANG’24 programming language consist of the following lexical elements:

* Literals:
    - numerical literals:
    A nonempty finite string of decimal digits (`0...9`) optionally preceded by a sign (`+` or `-`).

    - character literals:
        A character enclosed in single quotes (`'`). A character in a string literal can be specified by
        - any printable ASCII character, i.e., with ASCII code in range {32...126} but the single quote and the backslash must be preceded by the backslash (`\`),
        - a control sequence `\n` denoting the end of line
        - an ASCII code represented as \XX where X stands for any uppercase hexadecimal digit (`0...9` or `A...F`).
    - string literals:
        A possibly empty string of characters enclosed in double quotes (`"`). A character in a string literal can be specified by
            - any printable ASCII character, i.e., with ASCII code in range {`32...126`} but the double quote and the backslash must be preceded by the backslash (`\`),
            - a control sequence `\n` denoting the end of line
            - an ASCII code represented as `\XX` where X stands for any uppercase hexadecimal digit (`0...9` or `A...F`).
* Symbols:
```
( ) { } [ ] . , : ; == != < > <= >= * / % + - ^ =
```

* Identifiers:
    A nonempty finite string of letters (`A...Z` and `a...z`), decimal digits (0. . . 9), and underscores (_) that
        - starts with either a letter or an underscore and
        - is not a keyword or a constant.

* Keywords:
```
and bool char else if int nil none not or sizeof then return void while
```

* Comments:
    A string of characters starting with a hash (`#`) and extending to the end of line.

* White space:
    Space, horizontal tab (HT), line feed (LF) and carriage return (CR). Line feed alone denotes the end of line within a source file. Horizontal tabs are considered to be **8** spaces wide.

Lexical elements are recognized from left to right using the longest match approach.
