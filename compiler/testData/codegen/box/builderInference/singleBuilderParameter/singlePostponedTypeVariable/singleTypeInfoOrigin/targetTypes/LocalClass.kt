class Buildee<CT> {
    private lateinit var reference: CT & Any
    fun yield(arg: CT) { reference = arg as (CT & Any) }
    fun materialize(): CT = reference
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

// test 1: local class is outside the builder inference lambda
fun test1() {
    class Local { fun foo() {} }
    val buildee = build {
        yield(Local())
    }
    buildee.materialize().foo()
}

// test 2: local class is inside the builder inference lambda
fun test2() {
    val buildee = build {
        class Local { fun foo() {} }
        yield(Local())
    }
    buildee.materialize().foo()
}

fun box(): String {
    test1()
    test2()
    return "OK"
}
