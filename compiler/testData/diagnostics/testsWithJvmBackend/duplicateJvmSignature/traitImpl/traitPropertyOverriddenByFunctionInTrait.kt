// COMPARE_WITH_LIGHT_TREE

interface T {
    val x: Int
        get() = 1
}

interface C : T {
    <!ACCIDENTAL_OVERRIDE!>fun getX()<!> = 1
}
