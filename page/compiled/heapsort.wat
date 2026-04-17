(module
  (import "math" "random" (func $_math_random (result i64)))
  (import "env" "putchar" (func $_putchar (param i64)))
  (import "env" "putint" (func $_putint (param i64)))
  (memory (export "memory") 200)
  (global $SP (mut i64) (i64.const 8388608))
  (global $HP (mut i64) (i64.const 65536))
  
  (func $_math_abs (result i64)
    (local $T0 i64)
    (local $T1 i64)
    (local $target i32)
    ;; --- Prologue ---
    (local.set $T0 (global.get $SP))
    (global.set $SP (i64.sub (global.get $SP) (i64.const 24)))
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
                  ;; Label: _L8
                  (local.get $T0)
                  (i64.const 8)
                  i64.add
                  i32.wrap_i64
                  i64.load
                  (i64.const 0)
                  i64.lt_s
                  i64.extend_i32_u
                  i32.wrap_i64
                  (if
                    (then
                      (local.set $target (i32.const 3))
                      br $L
                    )
                    (else
                      (local.set $target (i32.const 1))
                      br $L
                    )
                  )
                )
                ;; Label: _L54
                (local.set $target (i32.const 2))
                br $L
              )
              ;; Label: _L11
              (local.get $T0)
              (i64.const 8)
              i64.add
              i32.wrap_i64
              i64.load
              (local.set $T1)
              (local.set $target (i32.const 5))
              br $L
            )
            ;; Label: _L10
            (local.get $T0)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const -1)
            i64.mul
            (local.set $T1)
            (local.set $target (i32.const 5))
            br $L
          )
          ;; Label: _L12
          (local.set $target (i32.const 5))
          (br $L)
        )
        ;; Label: _L9
      )
    )
    ;; --- Epilogue ---
    (global.set $SP (i64.add (global.get $SP) (i64.const 24)))
    (local.get $T1)
  )
  
  (func $_L0 (result i64)
    (local $T10 i64)
    (local $T13 i64)
    (local $T12 i64)
    (local $T3 i64)
    (local $T11 i64)
    (local $T2 i64)
    (local $target i32)
    ;; --- Prologue ---
    (local.set $T2 (global.get $SP))
    (global.set $SP (i64.sub (global.get $SP) (i64.const 48)))
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
                  ;; Label: _L15
                  (local.get $T2)
                  (i64.const -8)
                  i64.add
                  i32.wrap_i64
                  (local.get $T2)
                  (i64.const 8)
                  i64.add
                  i32.wrap_i64
                  i64.load
                  (i64.const 2)
                  i64.div_s
                  i64.store
                  (local.set $target (i32.const 1))
                  (br $L)
                )
                ;; Label: _L17
                (local.get $T2)
                (i64.const -8)
                i64.add
                i32.wrap_i64
                i64.load
                (i64.const 0)
                i64.gt_s
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
              ;; Label: _L55
              (local.set $target (i32.const 4))
              br $L
            )
            ;; Label: _L18
            (local.get $T2)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            (local.get $T2)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 1)
            i64.sub
            i64.store
            (local.get $T2)
            i32.wrap_i64
            i64.load
            (local.set $T10)
            (local.get $T2)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            i64.load
            (local.set $T11)
            (local.get $T2)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (local.set $T12)
            (global.get $SP)
            (i64.const 0)
            i64.add
            i32.wrap_i64
            (local.get $T10)
            i64.store
            (global.get $SP)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            (local.get $T11)
            i64.store
            (global.get $SP)
            (i64.const 16)
            i64.add
            i32.wrap_i64
            (local.get $T12)
            i64.store
            (call $_L1)
            (local.set $target (i32.const 1))
            br $L
          )
          ;; Label: _L19
          (i64.const 0)
          (local.set $T3)
          (local.set $target (i32.const 5))
          br $L
        )
        ;; Label: _L16
      )
    )
    ;; --- Epilogue ---
    (global.set $SP (i64.add (global.get $SP) (i64.const 48)))
    (local.get $T3)
  )
  
  (func $_L1 (result i64)
    (local $T17 i64)
    (local $T16 i64)
    (local $T14 i64)
    (local $T15 i64)
    (local $T4 i64)
    (local $T5 i64)
    (local $target i32)
    ;; --- Prologue ---
    (local.set $T4 (global.get $SP))
    (global.set $SP (i64.sub (global.get $SP) (i64.const 56)))
    (local.set $target (i32.const 0))
    (loop $L
      (block $B_exit
        (block $B_13
          (block $B_12
            (block $B_11
              (block $B_10
                (block $B_9
                  (block $B_8
                    (block $B_7
                      (block $B_6
                        (block $B_5
                          (block $B_4
                            (block $B_3
                              (block $B_2
                                (block $B_1
                                  (block $B_0
                                    (br_table $B_0 $B_1 $B_2 $B_3 $B_4 $B_5 $B_6 $B_7 $B_8 $B_9 $B_10 $B_11 $B_12 $B_13 (local.get $target))
                                  )
                                  ;; Label: _L20
                                  (local.get $T4)
                                  (i64.const -8)
                                  i64.add
                                  i32.wrap_i64
                                  (i64.const 2)
                                  (local.get $T4)
                                  (i64.const 8)
                                  i64.add
                                  i32.wrap_i64
                                  i64.load
                                  i64.mul
                                  (i64.const 1)
                                  i64.add
                                  i64.store
                                  (local.get $T4)
                                  (i64.const -8)
                                  i64.add
                                  i32.wrap_i64
                                  i64.load
                                  (local.get $T4)
                                  (i64.const 16)
                                  i64.add
                                  i32.wrap_i64
                                  i64.load
                                  i64.ge_s
                                  i64.extend_i32_u
                                  i32.wrap_i64
                                  (if
                                    (then
                                      (local.set $target (i32.const 2))
                                      br $L
                                    )
                                    (else
                                      (local.set $target (i32.const 1))
                                      br $L
                                    )
                                  )
                                )
                                ;; Label: _L56
                                (local.set $target (i32.const 3))
                                br $L
                              )
                              ;; Label: _L22
                              (i64.const 0)
                              (local.set $T5)
                              (local.set $target (i32.const 13))
                              br $L
                            )
                            ;; Label: _L24
                            (local.get $T4)
                            (i64.const -8)
                            i64.add
                            i32.wrap_i64
                            i64.load
                            (i64.const 1)
                            i64.add
                            (local.get $T4)
                            (i64.const 16)
                            i64.add
                            i32.wrap_i64
                            i64.load
                            i64.lt_s
                            i64.extend_i32_u
                            i32.wrap_i64
                            (if
                              (then
                                (local.set $target (i32.const 5))
                                br $L
                              )
                              (else
                                (local.set $target (i32.const 4))
                                br $L
                              )
                            )
                          )
                          ;; Label: _L57
                          (local.set $target (i32.const 9))
                          br $L
                        )
                        ;; Label: _L28
                        (local.get $T4)
                        i32.wrap_i64
                        i64.load
                        (i64.const 16)
                        i64.add
                        i32.wrap_i64
                        i64.load
                        i32.wrap_i64
                        (local.get $T4)
                        i32.wrap_i64
                        i64.load
                        (i64.const 16)
                        i64.add
                        i32.wrap_i64
                        i64.load
                        i32.wrap_i64
                        i64.load
                        (i64.const 1)
                        i64.add
                        i64.store
                        (local.get $T4)
                        i32.wrap_i64
                        i64.load
                        (i64.const 8)
                        i64.add
                        i32.wrap_i64
                        i64.load
                        (local.get $T4)
                        (i64.const -8)
                        i64.add
                        i32.wrap_i64
                        i64.load
                        (i64.const 1)
                        i64.add
                        (i64.const 8)
                        i64.mul
                        i64.add
                        i32.wrap_i64
                        i64.load
                        (local.get $T4)
                        i32.wrap_i64
                        i64.load
                        (i64.const 8)
                        i64.add
                        i32.wrap_i64
                        i64.load
                        (local.get $T4)
                        (i64.const -8)
                        i64.add
                        i32.wrap_i64
                        i64.load
                        (i64.const 8)
                        i64.mul
                        i64.add
                        i32.wrap_i64
                        i64.load
                        i64.gt_s
                        i64.extend_i32_u
                        i32.wrap_i64
                        (if
                          (then
                            (local.set $target (i32.const 7))
                            br $L
                          )
                          (else
                            (local.set $target (i32.const 6))
                            br $L
                          )
                        )
                      )
                      ;; Label: _L58
                      (local.set $target (i32.const 8))
                      br $L
                    )
                    ;; Label: _L25
                    (local.get $T4)
                    (i64.const -8)
                    i64.add
                    i32.wrap_i64
                    (local.get $T4)
                    (i64.const -8)
                    i64.add
                    i32.wrap_i64
                    i64.load
                    (i64.const 1)
                    i64.add
                    i64.store
                    (local.set $target (i32.const 8))
                    br $L
                  )
                  ;; Label: _L27
                  (local.set $target (i32.const 9))
                  br $L
                )
                ;; Label: _L30
                (local.get $T4)
                i32.wrap_i64
                i64.load
                (i64.const 16)
                i64.add
                i32.wrap_i64
                i64.load
                i32.wrap_i64
                (local.get $T4)
                i32.wrap_i64
                i64.load
                (i64.const 16)
                i64.add
                i32.wrap_i64
                i64.load
                i32.wrap_i64
                i64.load
                (i64.const 1)
                i64.add
                i64.store
                (local.get $T4)
                i32.wrap_i64
                i64.load
                (i64.const 8)
                i64.add
                i32.wrap_i64
                i64.load
                (local.get $T4)
                (i64.const 8)
                i64.add
                i32.wrap_i64
                i64.load
                (i64.const 8)
                i64.mul
                i64.add
                i32.wrap_i64
                i64.load
                (local.get $T4)
                i32.wrap_i64
                i64.load
                (i64.const 8)
                i64.add
                i32.wrap_i64
                i64.load
                (local.get $T4)
                (i64.const -8)
                i64.add
                i32.wrap_i64
                i64.load
                (i64.const 8)
                i64.mul
                i64.add
                i32.wrap_i64
                i64.load
                i64.lt_s
                i64.extend_i32_u
                i32.wrap_i64
                (if
                  (then
                    (local.set $target (i32.const 11))
                    br $L
                  )
                  (else
                    (local.set $target (i32.const 10))
                    br $L
                  )
                )
              )
              ;; Label: _L59
              (local.set $target (i32.const 12))
              br $L
            )
            ;; Label: _L31
            (local.get $T4)
            (i64.const -16)
            i64.add
            i32.wrap_i64
            (local.get $T4)
            i32.wrap_i64
            i64.load
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (local.get $T4)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 8)
            i64.mul
            i64.add
            i32.wrap_i64
            i64.load
            i64.store
            (local.get $T4)
            i32.wrap_i64
            i64.load
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (local.get $T4)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 8)
            i64.mul
            i64.add
            i32.wrap_i64
            (local.get $T4)
            i32.wrap_i64
            i64.load
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (local.get $T4)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 8)
            i64.mul
            i64.add
            i32.wrap_i64
            i64.load
            i64.store
            (local.get $T4)
            i32.wrap_i64
            i64.load
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (local.get $T4)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 8)
            i64.mul
            i64.add
            i32.wrap_i64
            (local.get $T4)
            (i64.const -16)
            i64.add
            i32.wrap_i64
            i64.load
            i64.store
            (local.get $T4)
            i32.wrap_i64
            i64.load
            (local.set $T14)
            (local.get $T4)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            i64.load
            (local.set $T15)
            (local.get $T4)
            (i64.const 16)
            i64.add
            i32.wrap_i64
            i64.load
            (local.set $T16)
            (global.get $SP)
            (i64.const 0)
            i64.add
            i32.wrap_i64
            (local.get $T14)
            i64.store
            (global.get $SP)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            (local.get $T15)
            i64.store
            (global.get $SP)
            (i64.const 16)
            i64.add
            i32.wrap_i64
            (local.get $T16)
            i64.store
            (call $_L1)
            (local.set $target (i32.const 12))
            br $L
          )
          ;; Label: _L33
          (i64.const 0)
          (local.set $T5)
          (local.set $target (i32.const 13))
          br $L
        )
        ;; Label: _L21
      )
    )
    ;; --- Epilogue ---
    (global.set $SP (i64.add (global.get $SP) (i64.const 56)))
    (local.get $T5)
  )
  
  (func $_heapsort (result i64)
    (local $T19 i64)
    (local $T20 i64)
    (local $T6 i64)
    (local $T7 i64)
    (local $T18 i64)
    (local $target i32)
    ;; --- Prologue ---
    (local.set $T6 (global.get $SP))
    (global.set $SP (i64.sub (global.get $SP) (i64.const 56)))
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
                  ;; Label: _L13
                  (global.get $SP)
                  (i64.const 0)
                  i64.add
                  i32.wrap_i64
                  (local.get $T6)
                  i64.store
                  (global.get $SP)
                  (i64.const 8)
                  i64.add
                  i32.wrap_i64
                  (i64.const 10)
                  i64.store
                  (call $_L0)
                  (local.get $T6)
                  (i64.const -8)
                  i64.add
                  i32.wrap_i64
                  (i64.const 9)
                  i64.store
                  (local.set $target (i32.const 1))
                  (br $L)
                )
                ;; Label: _L34
                (local.get $T6)
                (i64.const -8)
                i64.add
                i32.wrap_i64
                i64.load
                (i64.const 0)
                i64.gt_s
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
              ;; Label: _L60
              (local.set $target (i32.const 4))
              br $L
            )
            ;; Label: _L35
            (local.get $T6)
            (i64.const -16)
            i64.add
            i32.wrap_i64
            (local.get $T6)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 0)
            (i64.const 8)
            i64.mul
            i64.add
            i32.wrap_i64
            i64.load
            i64.store
            (local.get $T6)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 0)
            (i64.const 8)
            i64.mul
            i64.add
            i32.wrap_i64
            (local.get $T6)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (local.get $T6)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 8)
            i64.mul
            i64.add
            i32.wrap_i64
            i64.load
            i64.store
            (local.get $T6)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            i64.load
            (local.get $T6)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 8)
            i64.mul
            i64.add
            i32.wrap_i64
            (local.get $T6)
            (i64.const -16)
            i64.add
            i32.wrap_i64
            i64.load
            i64.store
            (local.get $T6)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            i64.load
            (local.set $T19)
            (global.get $SP)
            (i64.const 0)
            i64.add
            i32.wrap_i64
            (local.get $T6)
            i64.store
            (global.get $SP)
            (i64.const 8)
            i64.add
            i32.wrap_i64
            (i64.const 0)
            i64.store
            (global.get $SP)
            (i64.const 16)
            i64.add
            i32.wrap_i64
            (local.get $T19)
            i64.store
            (call $_L1)
            (local.get $T6)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            (local.get $T6)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 1)
            i64.sub
            i64.store
            (local.set $target (i32.const 1))
            br $L
          )
          ;; Label: _L36
          (i64.const 0)
          (local.set $T7)
          (local.set $target (i32.const 5))
          br $L
        )
        ;; Label: _L14
      )
    )
    ;; --- Epilogue ---
    (global.set $SP (i64.add (global.get $SP) (i64.const 56)))
    (local.get $T7)
  )
  
  (func $_main (result i64)
    (local $T31 i64)
    (local $T32 i64)
    (local $T24 i64)
    (local $T25 i64)
    (local $T8 i64)
    (local $T9 i64)
    (local $T30 i64)
    (local $T26 i64)
    (local $T27 i64)
    (local $T23 i64)
    (local $T22 i64)
    (local $T21 i64)
    (local $T28 i64)
    (local $T29 i64)
    (local $target i32)
    ;; --- Prologue ---
    (local.set $T8 (global.get $SP))
    (global.set $SP (i64.sub (global.get $SP) (i64.const 56)))
    (local.set $target (i32.const 0))
    (loop $L
      (block $B_exit
        (block $B_19
          (block $B_18
            (block $B_17
              (block $B_16
                (block $B_15
                  (block $B_14
                    (block $B_13
                      (block $B_12
                        (block $B_11
                          (block $B_10
                            (block $B_9
                              (block $B_8
                                (block $B_7
                                  (block $B_6
                                    (block $B_5
                                      (block $B_4
                                        (block $B_3
                                          (block $B_2
                                            (block $B_1
                                              (block $B_0
                                                (br_table $B_0 $B_1 $B_2 $B_3 $B_4 $B_5 $B_6 $B_7 $B_8 $B_9 $B_10 $B_11 $B_12 $B_13 $B_14 $B_15 $B_16 $B_17 $B_18 $B_19 (local.get $target))
                                              )
                                              ;; Label: _L37
                                              (i64.const 1024)
                                              i32.wrap_i64
                                              (i64.const 2024)
                                              i64.store
                                              (local.get $T8)
                                              (i64.const -8)
                                              i64.add
                                              i32.wrap_i64
                                              (i64.const 0)
                                              i64.store
                                              (local.set $target (i32.const 1))
                                              (br $L)
                                            )
                                            ;; Label: _L39
                                            (local.get $T8)
                                            (i64.const -8)
                                            i64.add
                                            i32.wrap_i64
                                            i64.load
                                            (i64.const 10)
                                            i64.lt_s
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
                                          ;; Label: _L61
                                          (local.set $target (i32.const 4))
                                          br $L
                                        )
                                        ;; Label: _L40
                                        (call $_math_random)
                                        (local.set $T21)
                                        (local.get $T21)
                                        (local.set $T22)
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
                                        (local.get $T22)
                                        i64.store
                                        (call $_math_abs)
                                        (local.set $T23)
                                        (i64.const 1032)
                                        (local.get $T8)
                                        (i64.const -8)
                                        i64.add
                                        i32.wrap_i64
                                        i64.load
                                        (i64.const 8)
                                        i64.mul
                                        i64.add
                                        i32.wrap_i64
                                        (local.get $T23)
                                        (i64.const 100)
                                        i64.rem_s
                                        i64.store
                                        (local.get $T8)
                                        (i64.const -8)
                                        i64.add
                                        i32.wrap_i64
                                        (local.get $T8)
                                        (i64.const -8)
                                        i64.add
                                        i32.wrap_i64
                                        i64.load
                                        (i64.const 1)
                                        i64.add
                                        i64.store
                                        (local.set $target (i32.const 1))
                                        br $L
                                      )
                                      ;; Label: _L41
                                      (local.get $T8)
                                      (i64.const -8)
                                      i64.add
                                      i32.wrap_i64
                                      (i64.const 0)
                                      i64.store
                                      (local.set $target (i32.const 5))
                                      (br $L)
                                    )
                                    ;; Label: _L45
                                    (local.get $T8)
                                    (i64.const -8)
                                    i64.add
                                    i32.wrap_i64
                                    i64.load
                                    (i64.const 10)
                                    i64.lt_s
                                    i64.extend_i32_u
                                    i32.wrap_i64
                                    (if
                                      (then
                                        (local.set $target (i32.const 7))
                                        br $L
                                      )
                                      (else
                                        (local.set $target (i32.const 6))
                                        br $L
                                      )
                                    )
                                  )
                                  ;; Label: _L62
                                  (local.set $target (i32.const 11))
                                  br $L
                                )
                                ;; Label: _L46
                                (i64.const 1032)
                                (local.get $T8)
                                (i64.const -8)
                                i64.add
                                i32.wrap_i64
                                i64.load
                                (i64.const 8)
                                i64.mul
                                i64.add
                                i32.wrap_i64
                                i64.load
                                (local.set $T24)
                                (local.get $T24)
                                (call $_putint)
                                (local.get $T8)
                                (i64.const -8)
                                i64.add
                                i32.wrap_i64
                                i64.load
                                (i64.const 9)
                                i64.lt_s
                                i64.extend_i32_u
                                i32.wrap_i64
                                (if
                                  (then
                                    (local.set $target (i32.const 9))
                                    br $L
                                  )
                                  (else
                                    (local.set $target (i32.const 8))
                                    br $L
                                  )
                                )
                              )
                              ;; Label: _L63
                              (local.set $target (i32.const 10))
                              br $L
                            )
                            ;; Label: _L42
                            (i64.const 44)
                            (call $_putchar)
                            (local.set $target (i32.const 10))
                            br $L
                          )
                          ;; Label: _L44
                          (local.get $T8)
                          (i64.const -8)
                          i64.add
                          i32.wrap_i64
                          (local.get $T8)
                          (i64.const -8)
                          i64.add
                          i32.wrap_i64
                          i64.load
                          (i64.const 1)
                          i64.add
                          i64.store
                          (local.set $target (i32.const 5))
                          br $L
                        )
                        ;; Label: _L47
                        (i64.const 10)
                        (call $_putchar)
                        (local.get $T8)
                        (i64.const -16)
                        i64.add
                        i32.wrap_i64
                        (i64.const 0)
                        i64.store
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
                        (i64.const 1032)
                        i64.store
                        (global.get $SP)
                        (i64.const 16)
                        i64.add
                        i32.wrap_i64
                        (local.get $T8)
                        (i64.const -16)
                        i64.add
                        i64.store
                        (call $_heapsort)
                        (local.get $T8)
                        (i64.const -8)
                        i64.add
                        i32.wrap_i64
                        (i64.const 0)
                        i64.store
                        (local.set $target (i32.const 12))
                        (br $L)
                      )
                      ;; Label: _L51
                      (local.get $T8)
                      (i64.const -8)
                      i64.add
                      i32.wrap_i64
                      i64.load
                      (i64.const 10)
                      i64.lt_s
                      i64.extend_i32_u
                      i32.wrap_i64
                      (if
                        (then
                          (local.set $target (i32.const 14))
                          br $L
                        )
                        (else
                          (local.set $target (i32.const 13))
                          br $L
                        )
                      )
                    )
                    ;; Label: _L64
                    (local.set $target (i32.const 18))
                    br $L
                  )
                  ;; Label: _L52
                  (i64.const 1032)
                  (local.get $T8)
                  (i64.const -8)
                  i64.add
                  i32.wrap_i64
                  i64.load
                  (i64.const 8)
                  i64.mul
                  i64.add
                  i32.wrap_i64
                  i64.load
                  (local.set $T29)
                  (local.get $T29)
                  (call $_putint)
                  (local.get $T8)
                  (i64.const -8)
                  i64.add
                  i32.wrap_i64
                  i64.load
                  (i64.const 9)
                  i64.lt_s
                  i64.extend_i32_u
                  i32.wrap_i64
                  (if
                    (then
                      (local.set $target (i32.const 16))
                      br $L
                    )
                    (else
                      (local.set $target (i32.const 15))
                      br $L
                    )
                  )
                )
                ;; Label: _L65
                (local.set $target (i32.const 17))
                br $L
              )
              ;; Label: _L48
              (i64.const 44)
              (call $_putchar)
              (local.set $target (i32.const 17))
              br $L
            )
            ;; Label: _L50
            (local.get $T8)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            (local.get $T8)
            (i64.const -8)
            i64.add
            i32.wrap_i64
            i64.load
            (i64.const 1)
            i64.add
            i64.store
            (local.set $target (i32.const 12))
            br $L
          )
          ;; Label: _L53
          (i64.const 10)
          (call $_putchar)
          (local.get $T8)
          (i64.const -16)
          i64.add
          i32.wrap_i64
          i64.load
          (local.set $T9)
          (local.set $target (i32.const 19))
          br $L
        )
        ;; Label: _L38
      )
    )
    ;; --- Epilogue ---
    (global.set $SP (i64.add (global.get $SP) (i64.const 56)))
    (local.get $T9)
  )
  (func (export "main") (result i64)
    (call $_main)
  )
)
