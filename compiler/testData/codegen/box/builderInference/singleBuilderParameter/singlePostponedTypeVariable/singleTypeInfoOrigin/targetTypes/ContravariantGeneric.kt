class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = In<Any>() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class In<in T>

fun consume(arg: In<Number>) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: In<Number>) {
    val buildee = build {
        yield(arg)
    }
    val result: In<Number> = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: In<Number>) {
    val buildee = build {
        consume(materialize())
    }
    val result: In<Number> = buildee.identity(arg)
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield(In<Any>())
    testMaterialize(In<Any>())
    return "OK"
}
