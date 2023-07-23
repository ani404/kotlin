// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlin-atomicfu-compiler-plugin-1.9.255-SNAPSHOT-nativeUpdated.jar

import kotlinx.atomicfu.*
import kotlin.test.*

class MultiInitTest {
    fun testBasic() {
        val t = MultiInit()
        check(t.incA() == 1)
        check(t.incA() == 2)
        check(t.incB() == 1)
        check(t.incB() == 2)
    }
}

class MultiInit {
    private val a = atomic(0)
    private val b = atomic(0)

    fun incA() = a.incrementAndGet()
    fun incB() = b.incrementAndGet()

    companion object {
        fun foo() {} // just to force some clinit in outer file
    }
}

@Test
fun box() {
    val testClass = MultiInitTest()
    testClass.testBasic()
}