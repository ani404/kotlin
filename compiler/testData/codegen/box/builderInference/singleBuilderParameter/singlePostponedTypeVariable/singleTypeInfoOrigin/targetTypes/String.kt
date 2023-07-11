class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = "42" as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

fun consume(arg: String) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: String) {
    val buildee = build {
        yield(arg)
    }
    val result: String = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: String) {
    val buildee = build {
        consume(materialize())
    }
    val result: String = buildee.identity(arg)
}

// test 3: cases with string literals
fun testStringLiterals() {
    fun test1() {
        val buildee = build {
            yield("42")
        }
        val result: String = buildee.identity("42")
    }
    fun test2() {
        val buildee = build {
            yield("""42""")
        }
        val result: String = buildee.identity("""42""")
    }
    fun test3() {
        val buildee = build {
            yield("${4}${2}")
        }
        val result: String = buildee.identity("${4}${2}")
    }
    test1()
    test2()
    test3()
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield("42")
    testMaterialize("42")
    testStringLiterals()
    return "OK"
}
