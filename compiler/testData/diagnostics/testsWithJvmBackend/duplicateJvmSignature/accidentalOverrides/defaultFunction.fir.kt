// COMPARE_WITH_LIGHT_TREE
// !DIAGNOSTICS: -UNUSED_PARAMETER

open class Base {
    open fun `foo$default`(d: Derived, i: Int, mask: Int, mh: Any) {}
}

class Derived : Base() {
    <!ACCIDENTAL_OVERRIDE{LT}!><!ACCIDENTAL_OVERRIDE{PSI}!>fun foo(i: Int = 0)<!> {}<!>
}
