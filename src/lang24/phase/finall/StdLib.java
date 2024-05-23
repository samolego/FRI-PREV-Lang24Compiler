package lang24.phase.finall;

import lang24.data.asm.AsmLine;

import java.util.LinkedList;
import java.util.List;

import static lang24.data.asm.AsmLine.TAB;

public class StdLib {
    public static final List<AsmLine> STD_LIB_INSTRS = new LinkedList<>();

    static {
        var putchar = List.of(
                "_putchar    LDB  $255,SP,#0F",
                "STB  $255,SP,8",
                "ADD $255,SP,8",  // Save address of the character to print, 8 is to skip static link
                "TRAP  0,Fputs,StdOut",  // Do syscall
                "POP  0,0"
        );
        genInstr(putchar);


        var getchar = List.of(
                "FgetsBuf    BYTE  0,0",
                "FgetsArgs    OCTA  FgetsBuf,2",
                "_getchar    ADD  $255,SP,8",
                "TRAP  0,Fgets,StdIn",
                "ADD  $255,SP,0",
                "LDB  $255,$255,0",
                "STB  $255,SP,7",  // Skip 7 bytes as characters are 8 bytes long in our lang
                "POP  0,0"
        );
        genInstr(getchar);


        var _new = List.of(
                "// Load size of the object to create",
                "_new    LDO  $0,SP,8",
                "// Store heap poointer in RV",
                "STOU  HP,SP,0",
                "// Increase heap pointer by the provided size",
                "ADD  HP,HP,$0",
                "POP  0,0"
        );
        genInstr(_new);


        var delete = List.of(
                "// Just return",
                "_delete    POP 0,0"
        );
        genInstr(delete);

        var exit = List.of(
                "// Load exit code",
                "_exit    LDO  $0,SP,8",
                "TRAP  0,Halt,$0"
        );
        genInstr(exit);


        // Compiler-generated code for others
        var putint = List.of(
                "// Prologue _putint, locals: 0, args: 16, temps: 0",
                "// Save current SP",
                "_putint    SET $0,SP",
                "// Subtract current sp by size locals + 2 * pointer size",
                "SUB SP,SP,16",
                "// Store old FP",
                "STO FP,SP,#8",
                "// Store return address",
                "GET FP,rJ",
                "STO FP,SP,#0",
                "// Set frame pointer to old SP",
                "SET FP,$0",
                "// Set new SP",
                "SUB SP,SP,16",
                "// End prologue",

                "SETL $0,#0008",
                "ADD $0,$253,$0",
                "LDOU $1,$0,0",
                "SETL $0,#0",
                "CMP $0,$1,$0",
                "ZSN $0,$0,1",
                "BZ $0,LPI",
                "SETL $0,#002D",
                "STOU $0,$254,8",
                "SETL $0,#021D",
                "STOU $0,$254,0",
                "PUSHJ $8,_putchar",
                "LDOU $0,$254,0",
                "ADD $0,$0,0",
                "SETL $0,#0008",
                "ADD $1,$253,$0",
                "SETL $0,#0008",
                "ADD $0,$253,$0",
                "LDOU $0,$0,0",
                "NEG $0,$0",
                "STOU $0,$1,0",
                "LPI    SETL $0,#0008",
                "ADD $0,$253,$0",
                "LDOU $1,$0,0",
                "SETL $0,#000A",
                "CMP $0,$1,$0",
                "ZSNN $0,$0,1",
                "BZ $0,LPI1",
                "SETL $0,#0008",
                "ADD $0,$253,$0",
                "LDOU $1,$0,0",
                "SETL $0,#000A",
                "DIV $0,$1,$0",
                "ADD $0,$0,0",
                "STOU $0,$254,8",
                "SETL $0,#021D",
                "STOU $0,$254,0",
                "PUSHJ $8,_putint",
                "LDOU $0,$254,0",
                "ADD $0,$0,0",
                "LPI1    SETL $0,#0008",
                "ADD $0,$253,$0",
                "LDOU $1,$0,0",
                "SETL $0,#000A",
                "DIV $0,$1,$0",
                "GET $1,rR",
                "SETL $0,#0030",
                "ADD $1,$1,$0",
                "SETL $0,#00FF",
                "DIV $0,$1,$0",
                "GET $0,rR",
                "ADD $0,$0,0",
                "STOU $0,$254,8",
                "SETL $0,#021D",
                "STOU $0,$254,0",
                "PUSHJ $8,_putchar",
                "LDOU $0,$254,0",
                "ADD $0,$0,0",
                "SETL $0,#0",
                "ADD $0,$0,0",
                "// Epilogue",
                "// Store return value on stack",
                "STO $0,FP,#0",
                "// Add to SP in order to then restore old FP and return address",
                "ADD SP,SP,16",
                "// Load & restore return address",
                "LDO $0,SP,#0",
                "PUT rJ,$0",
                "// Load old FP",
                "LDO $0,SP,#8",
                "// Restore SP",
                "SET SP,FP",
                "// Restore FP",
                "SET FP,$0",
                "POP 0,0",
                "// End epilogue"

        );
        genInstr(putint);
    }

    private static void genInstr(List<String> instrStrList) {
        var instr = instrStrList.stream()
                .map(s -> s.replaceAll(" {4}", TAB))
                .filter(s -> !s.isBlank())
                .map(s -> {
                    var split = s.split(TAB);
                    if (split.length == 2) {
                        return AsmLine.labeled(split[0], split[1]);
                    }
                    return AsmLine.instr(s);
                })
                .toList();

        STD_LIB_INSTRS.addAll(instr);
    }
}
