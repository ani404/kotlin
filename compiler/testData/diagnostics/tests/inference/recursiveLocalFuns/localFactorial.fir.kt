// FIR_IDE_IGNORE
// See KT-6271
fun foo() {
    fun fact(n: Int) = {
        if (n > 0) {
            <!INFERENCE_ERROR!>fact(n - 1)<!> <!UNRESOLVED_REFERENCE!>*<!> n
        }
        else {
            1
        }
    }
}
