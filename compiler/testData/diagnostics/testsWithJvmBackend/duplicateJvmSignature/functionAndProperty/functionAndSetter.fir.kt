// COMPARE_WITH_LIGHT_TREE
// !DIAGNOSTICS: -UNUSED_PARAMETER

class C {
    <!CONFLICTING_JVM_DECLARATIONS{LT}!><!CONFLICTING_JVM_DECLARATIONS{PSI}!>fun setX(x: Int)<!> {}<!>

    <!CONFLICTING_JVM_DECLARATIONS{LT}!><!CONFLICTING_JVM_DECLARATIONS{PSI}!>var x: Int<!> = 1<!>
}
