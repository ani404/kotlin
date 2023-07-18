// FIR_IDENTICAL

// MODULE: common
// TARGET_PLATFORM: Common
expect class A

// MODULE: intermediate()()(common)
// TARGET_PLATFORM: Common

actual class A(val ok: String)

// MODULE: main()()(intermediate)
// TARGET_PLATFORM: JS

fun A.ext(): String = ok

fun box(): String {
    val a = A("OK")
    return a.ext()
}
