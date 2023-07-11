class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = { _: Int -> } as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

fun consume(arg: (Int) -> Unit) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: (Int) -> Unit) {
    val buildee = build {
        yield(arg)
    }
    val result: (Int) -> Unit = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: (Int) -> Unit) {
    val buildee = build {
        consume(materialize())
    }
    val result: (Int) -> Unit = buildee.identity(arg)
}

// test 3: cases with function literals
fun testFunctionLiterals() {
    fun test1() {
        val buildee = build {
            yield { _: Int -> }
        }
        val result: (Int) -> Unit = buildee.identity({ _: Int -> })
    }
    fun test2() {
        val buildee = build {
            yield(fun(_: Int) {})
        }
        val result: (Int) -> Unit = buildee.identity(fun(_: Int) {})
    }
    test1()
    test2()
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield {}
    testMaterialize {}
    testFunctionLiterals()
    return "OK"
}
