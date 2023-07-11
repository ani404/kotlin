class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = Inv<Int>() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class Inv<T>

fun consume(arg: Inv<out Number>) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: Inv<out Number>) {
    val buildee = build {
        yield(arg)
    }
    val result: Inv<out Number> = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: Inv<out Number>) {
    val buildee = build {
        consume(materialize())
    }
    val result: Inv<out Number> = buildee.identity(arg)
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield(Inv<Int>())
    testMaterialize(Inv<Int>())
    return "OK"
}
