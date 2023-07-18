// FIR_IDENTICAL
// TARGET_BACKEND: JVM_IR

// FILE: priv/members/check/MyJClass.java
package priv.members.check;

public class MyJClass {
    public static String SSS = "";
}

// FILE: test.kt
fun test() {
    val d: priv.members.check.MyJClass
    d = priv.members.check.MyJClass()

    val o = priv.members.check.MyJClass.SSS
}
