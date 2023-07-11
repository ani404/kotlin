class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = 42 as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

fun consume(arg: Int) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: Int) {
    val buildee = build {
        yield(arg)
    }
    val result: Int = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: Int) {
    val buildee = build {
        consume(materialize())
    }
    val result: Int = buildee.identity(arg)
}

// test 3: cases with integer literals
fun testIntegerLiterals() {
    fun test1() {
        val buildee = build {
            yield(42)
        }
        val result: Int = buildee.identity(42)
    }
    fun test2() {
        val buildee = build {
            yield(0x42)
        }
        val result: Int = buildee.identity(0x42)
    }
    fun test3() {
        val buildee = build {
            yield(0b1000)
        }
        val result: Int = buildee.identity(0b1000)
    }
    test1()
    test2()
    test3()
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield(42)
    testMaterialize(42)
    testIntegerLiterals()
    return "OK"
}
