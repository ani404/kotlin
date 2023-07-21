/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.swift

@Suppress("UNUSED_PARAMETER")
sealed interface C {
    fun render(parent: C? = null): String

    interface Builder {
        val String.variable
            get() = C.Expression.Variable(this)

        val String.type
            get() = C.Type.Typedef(this)

        val String.literal
            get() = C.Expression.StringLiteral(this)

        val Number.literal
            get() = C.Expression.NumericLiteral(this)

        val nullptr
            get() = C.Expression.NULL
    }

    interface FileScopeBuilder: Builder {
        fun <T : C> T.build(): T
    }

    interface FunctionScopeBuilder: Builder {
        fun <T : C.Statement> T.build(): T
    }

    sealed interface Type : C {
        val precedence: Int
            get() = 0

        fun renderDeclaration(name: String? = null): String
        override fun render(parent: C?): String = renderDeclaration(null)

        fun Type.parenthesizeIfNecessary(name: String?) = name?.let { if (this.precedence > this@parenthesizeIfNecessary.precedence) "($name)" else name } ?: ""

        object Void : Type {
            override fun renderDeclaration(name: String?): String = "void" + name.indented
        }

        sealed interface Primitive : Type

        abstract class Modifier(val type: Primitive? = null, val displayName: String) : Primitive {
            override fun renderDeclaration(name: String?): String = displayName + type?.renderDeclaration(name).indented
        }

        class Short(type: Primitive? = null) : Modifier(type, "short")

        class Long(type: Primitive? = null) : Modifier(type, "long")

        class Signed(type: Primitive? = null) : Modifier(type, "signed")

        class Unsigned(type: Primitive? = null) : Modifier(type, "unsigned")

        enum class Numeric(val displayName: String) : Type {
            BOOL("_Bool"),
            CHAR("char"),
            INT("int"),
            FLOAT("float"),
            DOUBLE("double");

            override fun renderDeclaration(name: String?): String = displayName + name.indented
        }

        data class Const(val type: Type) : Type {
            override fun renderDeclaration(name: String?): String = type.renderDeclaration("const" + name.indented)
        }

        data class Volatile(val type: Type) : Type {
            override fun renderDeclaration(name: String?): String = type.renderDeclaration("volatile" + name.indented)
        }

        data class Atomic(val type: Type) : Type {
            override fun renderDeclaration(name: String?): String = type.renderDeclaration("_Atomic" + name.indented)
        }

        data class Struct(val name: String) : Type {
            override fun renderDeclaration(name: String?): String = "struct ${this.name}" + name.indented
        }

        data class Union(val name: String) : Type {
            override fun renderDeclaration(name: String?): String = "union ${this.name}" + name.indented
        }

        data class Enum(val name: String) : Type {
            override fun renderDeclaration(name: String?): String = "enum ${this.name}" + name.indented
        }

        data class Typedef(val name: String) : Type {
            override fun renderDeclaration(name: String?): String = "${this.name}" + name.indented
        }

        data class Pointer(val type: Type, val isRestrict: Boolean = false) : Type {
            override fun renderDeclaration(name: String?): String = type.renderDeclaration(
                    type.parenthesizeIfNecessary("*" + (if (isRestrict) "restrict " else "") + (name ?: ""))
            )

            override val precedence: Int
                get() = 3
        }

        data class Array(val type: Type, val size: Expression?) : Type {
            override fun renderDeclaration(name: String?): String = type.renderDeclaration(
                    type.parenthesizeIfNecessary(name) + "[${size?.render() ?: ""}]"
            )

            override val precedence: Int
                get() = 5
        }

        data class Function(val returnType: Type = Void, val argumentTypes: List<Declarator> = emptyList()) : Type {
            override fun renderDeclaration(name: String?): String {
                val arguments = "(${argumentTypes.joinToString { it.render() }})"
                return returnType.renderDeclaration((name ?: "") + arguments)
            }

            override val precedence: Int
                get() = 4
        }

        data class Attributed(val returnType: Type, val attributes: List<Attribute>) : Type {
            override fun renderDeclaration(name: String?): String = attributes
                    .joinToString { it.render() }
                    .let { "$it " } + returnType.renderDeclaration(name)
        }
    }

    sealed interface Expression : C {
        data class Variable(val name: String) : Expression {
            override fun render(parent: C?): String = name
        }

        data class StringLiteral(val value: String): Expression {
            override fun render(parent: C?): String = value
                    .replace("\"", "\\\"")
                    .replace("\\", "\\\\")
                    .let { "\"$it\"" }
        }

        data class NumericLiteral(val value: Number) : Expression {
            override fun render(parent: C?): String = value.toString()
        }

        object NULL : Expression {
            override fun render(parent: C?): String = "NULL"
        }

        data class Call(val receiver: Expression, val arguments: List<Expression>) : Expression {
            override fun render(parent: C?): String = receiver.render(parent) + arguments.joinToString { it.render(parent) }.let { "($it)" }
        }
    }

    data class Declarator(val type: Type, val name: String? = null, val attributes: List<Attribute> = emptyList()) : C {
        override fun render(parent: C?): String = type.renderDeclaration(
                listOfNotNull(
                        name,
                        attributes.takeIf { it.isNotEmpty() }?.joinToString(prefix = " ") { it.render(parent) }
                ).takeIf { it.isNotEmpty() }?.joinToString(separator = " ")
        )
    }

    data class Declaration(
            val declarators: List<Declarator>,
            val specifiersAndQualifiers: List<SpecifierOrQualifier> = emptyList(),
            val attributes: List<Attribute> = emptyList(),
    ) : C {
        sealed interface SpecifierOrQualifier : C

        enum class StorageQualifier(val displayName: String) : SpecifierOrQualifier {
            TYPEDEF("typedef"),
            CONSTEXPR("constexpr"),
            AUTO("auto"),
            REGISTER("register"),
            STATIC("static"),
            EXTERN("extern"),
            THREAD_LOCAL("_Thread_local");

            override fun render(parent: C?): String = displayName
        }

        enum class FunctionQualifier(val displayName: String) : SpecifierOrQualifier {
            INLINE("inline"),
            NORETURN("_Noreturn");

            override fun render(parent: C?): String = displayName
        }

        data class AlignAsSpecifier(val alignment: C) : SpecifierOrQualifier {
            override fun render(parent: C?): String = "_Alignas(${alignment.render(parent)})"
        }

        override fun render(parent: C?): String {
            val attributes = attributes.joinToString(separator = " ", postfix = "\n") { it.render(this) }
            val snq = specifiersAndQualifiers.joinToString(separator = " ", postfix = " ") { it.render(this) }
            val declarators = declarators.joinToString { it.render(this) }
            return "$attributes$snq$declarators;"
        }


//        data class Function(
//            val name: String,
//            val returnType: Type = C.Type.Primitive.VOID,
//            val parameters: List<Parameter> = emptyList(),
//            val attributes: List<Attribute> = emptyList(),
//        ) : Declaration {
//            override fun render(parent: C?): String {
//                val arguments = parameters.takeIf { it.isNotEmpty() }?.joinToString { it.render(this) }?.let { "($it)" } ?: "(void)"
//                val attributes = attributes.takeIf { it.isNotEmpty() }?.joinToString(separator = " ") { it.render(this) }.indented
//                return returnType.renderDeclaration(name + arguments + attributes)
//            }
//        }
//
//        data class Variable(
//            val name: String,
//            val type: Type,
//            val storageQualifiers: List<StorageQualifier> = emptyList(),
//            val attributes: List<Attribute> = emptyList()
//        ) : Declaration {
//            override fun render(parent: C?): String {
//                TODO("Not yet implemented")
//            }
//        }
    }

    sealed interface Statement : C {

    }

    data class Attribute(val arguments: List<Expression> = emptyList()) : C {
        override fun render(parent: C?): String = "__attribute__((${arguments.joinToString { it.render(parent) }}))"
    }

    class File : C {
        class Builder(var items: MutableList<C> = mutableListOf()) : FileScopeBuilder {
            override fun <T : C> T.build(): T = also { this@Builder.items.add(it) }
        }

        val items: MutableList<C>

        constructor(items: MutableList<C>) {
            this.items = items
        }

        constructor(block: Builder.() -> Unit) {
            this.items = Builder().apply(block).items
        }

        override fun render(parent: C?): String = items.joinToString(separator = "\n") { it.render(this).trim() }
    }
}

private val String?.indented: String
    get() = this?.let { " $it" } ?: ""

inline fun <T> MutableList<C>.add(body: C.FileScopeBuilder.() -> T): T {
    val builder = (object : C.FileScopeBuilder {
        val items: MutableList<C> = mutableListOf()
        override fun <T : C> T.build(): T = also { items.add(it) }
    })
    val result = builder.body()
    addAll(builder.items)
    return result
}

//region Types

val C.Builder.void
    get() = C.Type.Void

val C.Builder.bool
    get() = C.Type.Numeric.BOOL

val C.Builder.char
    get() = C.Type.Numeric.CHAR

val C.Builder.int
    get() = C.Type.Numeric.INT

val C.Builder.float
    get() = C.Type.Numeric.FLOAT

val C.Builder.double
    get() = C.Type.Numeric.DOUBLE

val C.Type.Primitive.short
    get() = C.Type.Short(this)

val C.Type.Primitive.long
    get() = C.Type.Short(this)

val C.Type.Primitive.signed
    get() = C.Type.Short(this)

val C.Type.Primitive.unsigned
    get() = C.Type.Short(this)

val C.Type.pointer
    get() = C.Type.Pointer(this)

val C.Type.const
    get() = C.Type.Const(this)

val C.Type.volatile
    get() = C.Type.Volatile(this)

fun C.Type.array(size: C.Expression? = null) = C.Type.Array(this, size)

fun C.Builder.functionType(returnType: C.Type = C.Type.Void) = C.Type.Function(returnType)

fun C.Type.function(vararg parameters: C.Type) = C.Type.Function(this, parameters.map { C.Declarator(it) })

fun C.Type.function(parameters: List<C.Declarator>) = C.Type.Function(this, parameters)

//endregion

//region Declarations

val C.Builder.typedef
    get() = C.Declaration.StorageQualifier.TYPEDEF
val C.Builder.constexpr
    get() = C.Declaration.StorageQualifier.CONSTEXPR
val C.Builder.auto
    get() = C.Declaration.StorageQualifier.AUTO
val C.Builder.register
    get() = C.Declaration.StorageQualifier.REGISTER
val C.Builder.static
    get() = C.Declaration.StorageQualifier.STATIC
val C.Builder.extern
    get() = C.Declaration.StorageQualifier.EXTERN
val C.Builder.thread_local
    get() = C.Declaration.StorageQualifier.THREAD_LOCAL
val C.Builder.inline
    get() = C.Declaration.FunctionQualifier.INLINE
val C.Builder.noreturn
    get() = C.Declaration.FunctionQualifier.NORETURN

fun C.Builder.declare(
        declarators: List<C.Declarator>,
        qualifiers: List<C.Declaration.SpecifierOrQualifier> = emptyList(),
        attributes: List<C.Attribute> = emptyList()
) = C.Declaration(declarators, qualifiers, attributes)

fun C.Builder.declare(
        vararg declarators: C.Declarator,
        qualifiers: List<C.Declaration.SpecifierOrQualifier> = emptyList(),
        attributes: List<C.Attribute> = emptyList()
) = C.Declaration(declarators.toList(), qualifiers, attributes)

fun C.Builder.function(
        returnType: C.Type = C.Type.Void,
        name: String,
        vararg arguments: C.Declarator,
        attributes: List<C.Attribute> = emptyList()
) = C.Declarator(C.Type.Function(returnType, arguments.toList()), name, attributes)

fun C.Builder.function(
        returnType: C.Type = C.Type.Void,
        name: String,
        arguments: List<C.Declarator>,
        attributes: List<C.Attribute> = emptyList()
) = C.Declarator(C.Type.Function(returnType, arguments), name, attributes)

fun C.Builder.variable(
        type: C.Type,
        name: String? = null,
        attributes: List<C.Attribute> = emptyList()
) = C.Declarator(type, name, attributes)

//endregion