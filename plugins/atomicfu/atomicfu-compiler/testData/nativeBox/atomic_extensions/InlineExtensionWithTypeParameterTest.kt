// FREE_COMPILER_ARGS: -Xplugin=/Users/Maria.Sokolova/IdeaProjects/kotlin/plugins/atomicfu/atomicfu-compiler/build/libs/kotlin-atomicfu-compiler-plugin-1.9.255-SNAPSHOT-nativeUpdated.jar

import kotlinx.atomicfu.*
import kotlin.test.*

class InlineExtensionWithTypeParameterTest {
    abstract class Segment<S : Segment<S>>(val id: Int)
    class SemaphoreSegment(id: Int) : Segment<SemaphoreSegment>(id)

    private inline fun <S : Segment<S>> AtomicRef<S>.foo(
        id: Int,
        startFrom: S
    ): Int {
        lazySet(startFrom)
        return value.getSegmentId()
    }

    private inline fun <S : Segment<S>> S.getSegmentId(): Int {
        var cur: S = this
        return cur.id
    }

    private val sref = atomic(SemaphoreSegment(0))

    fun testInlineExtensionWithTypeParameter() {
        val s = SemaphoreSegment(77)
        assertEquals(77, sref.foo(0, s))
    }
}

@Test
fun box() {
    val testClass = InlineExtensionWithTypeParameterTest()
    testClass.testInlineExtensionWithTypeParameter()
}
