// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlin-atomicfu-compiler-plugin-1.9.255-SNAPSHOT-nativeUpdated.jar

import kotlinx.atomicfu.locks.*
import kotlin.test.*

class ReentrantLockFieldTest {
    private val lock = reentrantLock()
    private var state = 0

    fun testLock() {
        lock.withLock {
            state = 1
        }
        assertEquals(1, state)
        assertTrue(lock.tryLock())
        lock.unlock()
    }
}

@Test
fun box() {
    val testClass = ReentrantLockFieldTest()
    testClass.testLock()
}