class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = { -> } as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

fun consume(arg: () -> Unit) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: () -> Unit) {
    val buildee = build {
        yield(arg)
    }
    val result: () -> Unit = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: () -> Unit) {
    val buildee = build {
        consume(materialize())
    }
    val result: () -> Unit = buildee.identity(arg)
}

// test 3: cases with function literals
fun testFunctionLiterals() {
    fun test1() {
        val buildee = build {
            yield { -> }
        }
        val result: () -> Unit = buildee.identity({ -> })
    }
    fun test2() {
        val buildee = build {
            yield(fun() {})
        }
        val result: () -> Unit = buildee.identity(fun() {})
    }
    fun test3() {
        val buildee = build {
            yield {}
        }
        val result: () -> Unit = buildee.identity({})
    }
    test1()
    test2()
    test3()
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield {}
    testMaterialize {}
    testFunctionLiterals()
    return "OK"
}
