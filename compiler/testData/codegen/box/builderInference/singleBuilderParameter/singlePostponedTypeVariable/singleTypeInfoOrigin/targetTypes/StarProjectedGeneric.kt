class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = Inv<Any?>() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class Inv<T>

fun consume(arg: Inv<*>) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: Inv<*>) {
    val buildee = build {
        yield(arg)
    }
    val result: Inv<*> = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: Inv<*>) {
    val buildee = build {
        consume(materialize())
    }
    val result: Inv<*> = buildee.identity(arg)
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield(Inv<Int>())
    testYield(Inv<Any>())
    testMaterialize(Inv<Int>())
    testMaterialize(Inv<Any>())
    return "OK"
}
