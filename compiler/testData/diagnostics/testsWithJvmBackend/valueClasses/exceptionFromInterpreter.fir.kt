// COMPARE_WITH_LIGHT_TREE
// !RENDER_ALL_DIAGNOSTICS_FULL_TEXT
// TARGET_BACKEND: JVM_IR
// !DIAGNOSTICS: -CONST_VAL_WITH_NON_CONST_INITIALIZER, -DIVISION_BY_ZERO
// WITH_STDLIB

const val divideByZero = <!EXCEPTION_IN_CONST_VAL_INITIALIZER{PSI}!>1 <!EXCEPTION_IN_CONST_VAL_INITIALIZER{LT}!>/ 0<!><!>
val disivionByZeroWarn = <!EXCEPTION_IN_CONST_EXPRESSION{PSI}!>1 <!EXCEPTION_IN_CONST_EXPRESSION{LT}!>/ 0<!><!>
const val trimMarginException = "123".<!EXCEPTION_IN_CONST_VAL_INITIALIZER!>trimMargin(" ")<!>

// TODO must report all these exceptions directly from fir2ir
//annotation class A(val i: Int, val b: Int)
//
//@A(1 / 0, 2)
//fun foo() {}
