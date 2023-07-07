// WITH_STDLIB
// WITH_REFLECT
// TARGET_BACKEND: JVM_IR

package kotlinx.serialization.internal

import kotlin.annotation.*
import kotlinx.serialization.*
import kotlin.reflect.KClass

/*
  Until the annotation is added to the serialization runtime,
  we have to create an annotation with that name in the project itself
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Named


@Serializable
class Plain(val i: Int)

@Serializable
object Object


@Serializable
class WithStd(val i: Int) {
    companion object {
        var j: Int = 42
    }
}

@Serializable
class WithNamed(val i: Int) {
    companion object MyCompanion {
        var j: Int = 42
    }
}

fun box(): String {
    if (Plain::class.annotations.any { it.annotationClass.simpleName == "Named" }) {
        return "Annotation on Plain class"
    }
    if (Plain.Companion::class.annotations.any { it.annotationClass.simpleName == "Named" }) {
        return "Annotation on Plain.Companion class"
    }

    if (Object::class.annotations.any { it.annotationClass.simpleName == "Named" }) {
        return "Annotation on Object"
    }

    if (WithStd::class.annotations.any { it.annotationClass.simpleName == "Named" }) {
        return "Annotation on WithStd class"
    }
    if (WithStd.Companion::class.annotations.any { it.annotationClass.simpleName == "Named" }) {
        return "Annotation on WithStd.Companion class"
    }

    if (WithNamed::class.annotations.any { it.annotationClass.simpleName == "Named" }) {
        return "Annotation on WithNamed class"
    }
    if (WithNamed.MyCompanion::class.annotations.none { it.annotationClass.simpleName == "Named" }) {
        return "Missed annotation on WithNamed.MyCompanion class ${WithNamed.MyCompanion::class.annotations}"
    }

    return "OK"
}
