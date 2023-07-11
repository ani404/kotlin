class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = Enumeration.ENTRY as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

enum class Enumeration { ENTRY }

fun consume(arg: Enumeration) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: Enumeration) {
    val buildee = build {
        yield(arg)
    }
    val result: Enumeration = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: Enumeration) {
    val buildee = build {
        consume(materialize())
    }
    val result: Enumeration = buildee.identity(arg)
}

// test 3: a case with an enum entry
fun testEnumEntry() {
    val buildee = build {
        yield(Enumeration.ENTRY)
    }
    val result: Enumeration = buildee.identity(Enumeration.ENTRY)
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield(Enumeration.ENTRY)
    testMaterialize(Enumeration.ENTRY)
    return "OK"
}
