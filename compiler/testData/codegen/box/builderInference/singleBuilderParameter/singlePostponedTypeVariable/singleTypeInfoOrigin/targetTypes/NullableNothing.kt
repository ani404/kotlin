class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = null as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

fun consume(arg: Nothing?) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: Nothing?) {
    val buildee = build {
        yield(arg)
    }
    val result: Nothing? = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: Nothing?) {
    val buildee = build {
        consume(materialize())
    }
    val result: Nothing? = buildee.identity(arg)
}

// test 3: a case with a null literal
fun testNullLiteral() {
    val buildee = build {
        yield(null)
    }
    val result: Nothing? = buildee.identity(null)
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield(null)
    testMaterialize(null)
    testNullLiteral()
    return "OK"
}
