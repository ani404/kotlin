// COMPARE_WITH_LIGHT_TREE
// WITH_STDLIB

@JvmInline
value class <!CONFLICTING_JVM_DECLARATIONS!>A(val x: Int)<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: UInt)<!>: this(x.toInt())
}

data class <!CONFLICTING_JVM_DECLARATIONS!>B(val x: UInt)<!> {
    <!CONFLICTING_JVM_DECLARATIONS!>constructor(x: Int)<!> : this(x.toUInt())
}
