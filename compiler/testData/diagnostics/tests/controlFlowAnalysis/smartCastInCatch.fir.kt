// !SKIP_TXT

import kotlin.reflect.KClass

fun exc(flag: Boolean) {
    if (flag) throw RuntimeException()
}

fun test(flag: Boolean) {
    var x: Any?
    x = ""
    try {
        x = null
        exc(flag)
        x = 1
        exc(!flag)
        x = ""
    } catch (e: Throwable) {
        // all bad - could come here from either call
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun testGetClassThrows() {
    var x: KClass<String>? = String::class
    x as KClass<String>
    try {
        x = null
        x = String::class
    } catch (e: Throwable) {
        // bad - get class call can throw
        x<!UNSAFE_CALL!>.<!>simpleName
    }
}
