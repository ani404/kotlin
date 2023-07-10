class Buildee<CT> {
    fun yield(arg: CT) {}
    fun materialize(): CT = null!!
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass

fun consume(arg: UserKlass) {}

// test 1: PTV is in consuming position (yield-case)
fun testYield(arg: UserKlass) {
    build(fun(it) {
        it.yield(arg)
    })
}

// test 2: PTV is in producing position (materialize-case)
fun testMaterialize(arg: UserKlass) {
    build(fun(it) {
        consume(it.materialize())
    })
}
