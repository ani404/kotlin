// WITH_STDLIB

class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = suspend {} as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

fun consume(arg: suspend () -> Unit) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: suspend () -> Unit) {
    val buildee = build {
        yield(arg)
    }
    val result: suspend () -> Unit = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: suspend () -> Unit) {
    val buildee = build {
        consume(materialize())
    }
    val result: suspend () -> Unit = buildee.identity(arg)
}

// test 3: a case with a function literal
fun testFunctionLiterals() {
    val buildee = build {
        yield(suspend {})
    }
    val result: suspend () -> Unit = buildee.identity(suspend {})
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield {}
    testMaterialize {}
    testFunctionLiterals()
    return "OK"
}
