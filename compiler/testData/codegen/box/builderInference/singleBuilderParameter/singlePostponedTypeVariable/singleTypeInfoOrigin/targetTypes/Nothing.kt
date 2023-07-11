class Buildee<CT> {
    fun yield(arg: CT) {}
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

fun consume(arg: Nothing) {}

fun test() {
    val buildee = build {
        try {
            yield(throw Throwable())
        } catch (_: Throwable) {}
    }
    try {
        val result: Nothing = buildee.identity(throw Throwable())
    } catch (_: Throwable) {}
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    test()
    return "OK"
}
