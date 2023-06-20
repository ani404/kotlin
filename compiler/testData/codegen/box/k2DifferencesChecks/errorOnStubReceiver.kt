// WITH_STDLIB
// FIR_DUMP
// SKIP_TXT

fun Any?.test() {}

class Bar {
    fun test() {}
}

fun main() {

    buildList {
        add(Bar())
        this.get(0).test() // resolved to Any?.test
    }
    buildList<Bar> {
        add(Bar())
        this.get(0).test() // resolved to Bar.test
    }
}


fun box() = "OK"
