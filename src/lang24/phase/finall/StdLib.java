package lang24.phase.finall;

import lang24.data.asm.AsmLine;

import java.util.List;

import static lang24.data.asm.AsmLine.TAB;

public class StdLib {
    private static final String STR_TAB = String.valueOf(TAB);
    public static final List<AsmLine> PUTCHAR;

    static {
        var putchar = List.of(
                "_putchar    LDB  $255,SP,#0F",
                "STB  $255,SP,8",
                "ADD $255,SP,8",  // Save address of the character to print, 8 is to skip static link
                "TRAP  0,Fputs,StdOut",  // Do syscall
                "POP  0,0"
        );
        PUTCHAR = putchar.stream()
                .map(s -> s.replaceAll(" {4}", STR_TAB))
                .filter(s -> !s.isBlank())
                .map(s -> {
                    var split = s.split(STR_TAB);
                    if (split.length == 2) {
                        return AsmLine.labeled(split[0], split[1]);
                    }
                    return AsmLine.instr(split[0]);
                })
                .toList();
    }
}
