class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = 42 as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class Context<T> {
    fun consume(arg: T & Any) {}

    // test 1: PTV is in consuming position (yield-case)
    fun testYield(arg: T & Any) {
        val buildee = build {
            yield(arg)
        }
        val result: T & Any = buildee.identity(arg)
    }

    // test 2: PTV is in producing position (materialize-case)
    fun testMaterialize(arg: T & Any) {
        val buildee = build {
            consume(materialize())
        }
        val result: T & Any = buildee.identity(arg)
    }
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    with(Context<Int?>()) {
        testYield(42)
        testMaterialize(42)
    }
    return "OK"
}
