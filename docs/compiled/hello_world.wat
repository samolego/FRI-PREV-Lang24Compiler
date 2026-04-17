(module
  (import "env" "putchar" (func $_putchar (param i64)))
  (memory (export "memory") 200)
  (global $SP (mut i64) (i64.const 8388608))
  (global $HP (mut i64) (i64.const 65536))
  (data (i32.const 1024) "\48\00\00\00\00\00\00\00\65\00\00\00\00\00\00\00\6c\00\00\00\00\00\00\00\6c\00\00\00\00\00\00\00\6f\00\00\00\00\00\00\00\2c\00\00\00\00\00\00\00\20\00\00\00\00\00\00\00\57\00\00\00\00\00\00\00\6f\00\00\00\00\00\00\00\72\00\00\00\00\00\00\00\6c\00\00\00\00\00\00\00\64\00\00\00\00\00\00\00\21\00\00\00\00\00\00\00\0a\00\00\00\00\00\00\00\00\00\00\00\00\00\00\00")
  
  (func $_main (result i64)
    (local $T1 i64)
    (local $T5 i64)
    (local $T4 i64)
    (local $T0 i64)
    (local $target i32)
    ;; --- Prologue ---
    (local.set $T0 (global.get $SP))
    (global.set $SP (i64.sub (global.get $SP) (i64.const 40)))
    (local.set $target (i32.const 0))
    (loop $L
      (block $B_exit
        (block $B_1
          (block $B_0
            (br_table $B_0 $B_1 (local.get $target))
          )
          ;; Label: _L3
          (local.get $T0)
          (i64.const -8)
          i64.add
          i32.wrap_i64
          (i64.const 1024)
          i64.store
          (local.get $T0)
          (i64.const -8)
          i64.add
          i32.wrap_i64
          i64.load
          (local.set $T4)
          (global.get $SP)
          (i64.const 0)
          i64.add
          i32.wrap_i64
          (i64.const 541)
          i64.store
          (global.get $SP)
          (i64.const 8)
          i64.add
          i32.wrap_i64
          (local.get $T4)
          i64.store
          (call $_printStr)
          (i64.const 0)
          (local.set $T1)
          (local.set $target (i32.const 1))
          br $L
        )
        ;; Label: _L4
      )
    )
    ;; --- Epilogue ---
    (global.set $SP (i64.add (global.get $SP) (i64.const 40)))
    (local.get $T1)
  )
  
  (func $_printStr (result i64)
    (local $T7 i64)
    (local $T2 i64)
    (local $T3 i64)
    (local $T6 i64)
    (local $target i32)
    ;; --- Prologue ---
    (local.set $T2 (global.get $SP))
    (global.set $SP (i64.sub (global.get $SP) (i64.const 32)))
    (local.set $target (i32.const 0))
    (loop $L
      (block $B_exit
        (block $B_5
          (block $B_4
            (block $B_3
              (block $B_2
                (block $B_1
                  (block $B_0
                    (br_table $B_0 $B_1 $B_2 $B_3 $B_4 $B_5 (local.get $target))
                  )
                  ;; Label: _L5
                  (local.set $target (i32.const 1))
                  (br $L)
                )
                ;; Label: _L7
                (local.get $T2)
                (i64.const 8)
                i64.add
                i32.wrap_i64
                i64.load
                i32.wrap_i64
                i64.load
                (i64.const 0)
                i64.ne
                i64.extend_i32_u
                i32.wrap_i64
                (if
                  (then
                    (local.set $target (i32.const 3))
                    br $L
                  )
                  (else
                    (local.set $target (i32.const 2))
                    br $L
                  )
                )
              )
              ;; Label: _L10
              (local.set $target (i32.const 4))
              br $L
            )
            ;; Label: _L8
            (local.get $T2)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            i32.wrap_i64
            i64.load
            (local.set $T6)
            (local.get $T6)
            (call $_putchar)
            (local.get $T2)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            (local.get $T2)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 8)
            i64.add
            i64.store
            (local.set $target (i32.const 1))
            br $L
          )
          ;; Label: _L9
          (i64.const 0)
          (local.set $T3)
          (local.set $target (i32.const 5))
          br $L
        )
        ;; Label: _L6
      )
    )
    ;; --- Epilogue ---
    (global.set $SP (i64.add (global.get $SP) (i64.const 32)))
    (local.get $T3)
  )
  (func (export "main") (result i64)
    (call $_main)
  )
)
