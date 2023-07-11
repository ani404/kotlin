class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = Out<Int>() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class Out<out T>

fun consume(arg: Out<Number>) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: Out<Number>) {
    val buildee = build {
        yield(arg)
    }
    val result: Out<Number> = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: Out<Number>) {
    val buildee = build {
        consume(materialize())
    }
    val result: Out<Number> = buildee.identity(arg)
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield(Out<Int>())
    testMaterialize(Out<Int>())
    return "OK"
}
