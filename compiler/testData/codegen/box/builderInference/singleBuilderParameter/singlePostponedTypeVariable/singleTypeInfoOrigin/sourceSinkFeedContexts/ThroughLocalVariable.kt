class Buildee<CT> {
    fun materialize(): CT = UserKlass() as CT
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

class UserKlass

fun consume(arg: UserKlass) {}

fun test(arg: UserKlass) {
    val buildee = build {
        val temp = materialize()
        consume(temp)
    }
    val result: UserKlass = buildee.identity(arg)
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    test(UserKlass())
    return "OK"
}
