class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = UserKlass() as CT
}

fun <FT> build(
    instructions: (Buildee<FT>) -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass

fun consume(arg: UserKlass) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: UserKlass) {
    val buildee = build {
        it.yield(arg)
    }
    val result: UserKlass = buildee.identity(arg)
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: UserKlass) {
    val buildee = build {
        consume(it.materialize())
    }
    val result: UserKlass = buildee.identity(arg)
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    testYield(UserKlass())
    testMaterialize(UserKlass())
    return "OK"
}
