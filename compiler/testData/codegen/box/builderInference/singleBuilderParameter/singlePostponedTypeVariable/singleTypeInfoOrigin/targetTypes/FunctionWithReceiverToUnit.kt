class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = fun Int.() {} as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

fun consume(arg: Int.() -> Unit) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: Int.() -> Unit) {
    val buildee = build {
        yield(arg)
    }
    val result: Int.() -> Unit = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: Int.() -> Unit) {
    val buildee = build {
        consume(materialize())
    }
    val result: Int.() -> Unit = buildee.identity(arg)
}

// test 3: a case with a function literal
fun testFunctionLiterals() {
    val buildee = build {
        yield(fun Int.() {})
    }
    val result: Int.() -> Unit = buildee.identity(fun Int.() {})
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield {}
    testMaterialize {}
    testFunctionLiterals()
    return "OK"
}
