# FX-7000G BASIC — Test Plan

## 1. Philosophy

Every implemented BASIC feature gets a small deterministic test.

The test suite should eventually cover:

- tokenizer
- parser
- runtime
- editor
- program storage
- keyboard mapping
- display abstraction

Manual calculator-style tests are also valuable.

## 2. First smoke test

Program:

    10 PRINT "HELLO"
    20 END

Expected:

    HELLO

## 3. Assignment

    10 A=10
    20 B=A*2
    30 PRINT B
    40 END

Expected:

    20

## 4. Expression precedence

    10 A=2+3*4
    20 PRINT A
    30 END

Expected:

    14

## 5. Parentheses

    10 A=(2+3)*4
    20 PRINT A
    30 END

Expected:

    20

## 6. GOTO

    10 A=1
    20 PRINT A
    30 GOTO 50
    40 PRINT 999
    50 PRINT 2
    60 END

Expected output:

    1
    2

## 7. IF

    10 A=10
    20 IF A=10 THEN GOTO 40
    30 PRINT 999
    40 PRINT A
    50 END

Expected:

    10

## 8. FOR/NEXT

    10 FOR I=1 TO 5
    20 PRINT I
    30 NEXT I
    40 END

Expected:

    1
    2
    3
    4
    5

## 9. FOR/NEXT with STEP

    10 FOR I=10 TO 2 STEP -2
    20 PRINT I
    30 NEXT I
    40 END

Expected:

    10
    8
    6
    4
    2

## 10. GOSUB/RETURN

    10 GOSUB 100
    20 PRINT 2
    30 END
    100 PRINT 1
    110 RETURN

Expected:

    1
    2

## 11. INPUT

    10 INPUT A
    20 PRINT A
    30 END

Manual test:

- run program
- enter a number
- verify it is assigned and printed

## 12. Program editing

Verify:

1. Enter line 10.
2. Enter line 20.
3. Re-enter line 20 with different contents.
4. Verify replacement.
5. Enter `20` alone.
6. Verify deletion.
7. LIST.
8. Verify correct ordering.

## 13. Program slots

Verify:

- P0-P9 exist
- programs remain independent
- selecting P0 does not alter P1
- occupied slots are shown correctly
- empty slots remain empty

## 14. Tokenization

Verify that:

    PRINT

is represented internally by one PRINT token.

Verify that deleting PRINT removes the whole token rather than five character nodes.

## 15. Keyboard

For every Tier-1 key:

- press key in PROG INPUT
- verify correct token appears
- verify SHIFT/ALPHA combinations do not collide
- verify NORMAL calculator mode remains unchanged

## 16. Display

Verify:

- DEG remains normal calculator status
- PRG appears in programming mode
- P0123456789 appears in programming mode
- occupied slots use `*`
- BASIC output scrolls correctly
- long source lines remain editable

## 17. Graphics

Initial tests:

    CLS
    PLOT 10,10

    CLS
    LINE 10,10,100,50

    CLS
    BOX 10,10,100,50

    CLS
    CIRCLE 50,50,20

Exact pixel appearance should be validated against the display abstraction, not against an FX-880P screen.

## 18. Regression rule

Existing FX-7000G calculator functionality must continue to work exactly as before outside PROG mode.

Entering and leaving PROG mode must not corrupt:

- calculator variables
- calculator display state
- angle mode
- existing graphing state
- normal key mappings
