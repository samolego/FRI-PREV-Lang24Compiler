package lang24.phase.watgen;

import lang24.data.wat.WatWriter;

public class WatStdLib {
    public static void genStdLib(WatWriter writer) {
        // Use groupStart to let WatWriter handle formatting
        writer.groupStart("(func $_putchar (param $c i64) (result i64)");
        writer.println("(call $putchar (i32.wrap_i64 (local.get $c)))");
        writer.println("(i64.const 0)");
        writer.groupEnd();

        writer.groupStart("(func $_getchar (result i64)");
        writer.println("(i64.extend_i32_s (call $getchar))");
        writer.groupEnd();

        writer.groupStart("(func $_putint (param $n i64) (result i64)");
        writer.println("(local $val i64)");
        writer.println("(local.set $val (local.get $n))");
        writer.groupStart("(if (i64.lt_s (local.get $val) (i64.const 0))");
        writer.groupStart("(then");
        writer.println("(call $putchar (i32.const 45))");
        writer.println("(local.set $val (i64.sub (i64.const 0) (local.get $val)))");
        writer.groupEnd();
        writer.groupEnd();
        writer.groupStart("(if (i64.ge_s (local.get $val) (i64.const 10))");
        writer.groupStart("(then");
        writer.println("(drop (call $_putint (i64.div_s (local.get $val) (i64.const 10))))");
        writer.groupEnd();
        writer.groupEnd();
        writer.println("(call $putchar (i32.wrap_i64 (i64.add (i64.rem_s (local.get $val) (i64.const 10)) (i64.const 48))))");
        writer.println("(i64.const 0)");
        writer.groupEnd();

        writer.groupStart("(func $_new (param $size i64) (result i64)");
        writer.println("(local $addr i64)");
        writer.println("(local.set $addr (global.get $HP))");
        writer.println("(global.set $HP (i64.add (global.get $HP) (local.get $size)))");
        writer.println("(local.get $addr)");
        writer.groupEnd();

        writer.groupStart("(func $_exit (param $code i64) (result i64)");
        writer.println("(call $exit (i32.wrap_i64 (local.get $code)))");
        writer.println("(i64.const 0)");
        writer.groupEnd();
    }
}
