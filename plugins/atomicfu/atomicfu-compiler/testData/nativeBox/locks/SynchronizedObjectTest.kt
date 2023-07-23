// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlin-atomicfu-compiler-plugin-1.9.255-SNAPSHOT-nativeUpdated.jar

import kotlinx.atomicfu.locks.*
import kotlin.test.*

class SynchronizedObjectTest : SynchronizedObject() {

    fun testSync() {
        val result = synchronized(this) { bar() }
        assertEquals("OK", result)
    }

    private fun bar(): String =
        synchronized(this) {
            "OK"
        }
}

@Test
fun box() {
    val testClass = SynchronizedObjectTest()
    testClass.testSync()
}