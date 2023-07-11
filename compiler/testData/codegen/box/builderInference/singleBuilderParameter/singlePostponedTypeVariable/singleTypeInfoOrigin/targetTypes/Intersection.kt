class Buildee<CT> {
    fun yield(arg: CT) {}
}

fun <FT> build(
    instructions: Buildee<FT>.() -> Unit
): Buildee<FT> {
    return Buildee<FT>().apply(instructions)
}

interface I1
interface I2
object A: I1, I2
object B: I1, I2

fun <T> select(vararg arg: T): T = arg[0]

fun test() {
    var intersection = select(A, B)
    val buildee = build {
        yield(intersection)
    }
    intersection = buildee.identity(intersection)
}

fun <T> Buildee<T>.identity(arg: T): T = arg

fun box(): String {
    test()
    return "OK"
}
