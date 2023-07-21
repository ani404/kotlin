/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.swift

import io.outfoxx.swiftpoet.*
import org.jetbrains.kotlin.backend.konan.llvm.KonanBinaryInterface
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.builtins.UnsignedType
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.explicitParameters
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Generate a Swift API file for the given Kotlin IR module.
 *
 * A temporary solution to kick-start the work on Swift Export.
 * A proper solution is likely to be FIR-based and will be added later
 * as it requires a bit more work.
 *
 */
class IrBasedSwiftGenerator(private val moduleName: String) : IrElementVisitorVoid {
    val declarations = mutableListOf<Swift.Declaration>()

    private val initRuntimeIfNeededSpec = declarations.add {
        function(
                "initRuntimeIfNeeded",
                attributes = listOf(attribute("_silgen_name", "Kotlin_initRuntimeIfNeeded".literal)),
                visibility = Swift.Declaration.Visibility.PRIVATE,
        ).declare()
    }

    fun build(): String = Swift.File(listOf(Swift.Import.Module("Foundation")), declarations).render()

    override fun visitElement(element: IrElement) {
        element.acceptChildrenVoid(this)
    }

    override fun visitFunction(declaration: IrFunction) {
        if (!isSupported(declaration)) {
            return
        }

        val name = declaration.name.identifier
        val symbolName = with(KonanBinaryInterface) { declaration.symbolName }

        declarations.add {
            val returnType = mapType(declaration.returnType) ?: return
            val parameters = declaration.explicitParameters.map { parameter(it.name.asString(), type = mapType(it.type) ?: return) }

            val forwardDeclaration = function(
                    "${name}_bridge",
                    parameters = parameters,
                    type = returnType,
                    attributes = listOf(attribute("_silgen_name", symbolName.literal))
            ).declare()

            function(
                    name,
                    parameters = parameters,
                    type = returnType,
                    visibility = Swift.Declaration.Visibility.PUBLIC
            ) {
                initRuntimeIfNeededSpec.name.variable.call().state()
                `return`(forwardDeclaration.name.variable.call(declaration.explicitParameters.map {
                    it.name.asString() of it.name.asString().variable
                }))
            }.declare()
        }
    }

    private fun isSupported(declaration: IrFunction): Boolean {
        // No Kotlin-exclusive stuff
        return declaration.visibility.isPublicAPI
                && declaration.extensionReceiverParameter == null
                && declaration.dispatchReceiverParameter == null
                && declaration.contextReceiverParametersCount == 0
                && !declaration.isExpect
                && !declaration.isInline
    }

    private fun mapType(declaration: IrType): Swift.Type? {
        val swiftPrimitiveTypeName: String? = declaration.getPrimitiveType().takeUnless { declaration.isNullable() }?.let {
            when (it) {
                PrimitiveType.BYTE -> "Int8"
                PrimitiveType.BOOLEAN -> "Bool"
                PrimitiveType.CHAR -> null
                PrimitiveType.SHORT -> "Int16"
                PrimitiveType.INT -> "Int32"
                PrimitiveType.LONG -> "Int64"
                PrimitiveType.FLOAT -> "Float"
                PrimitiveType.DOUBLE -> "Double"
            }
        } ?: declaration.getUnsignedType().takeUnless { declaration.isNullable() }?.let {
            when (it) {
                UnsignedType.UBYTE -> "UInt8"
                UnsignedType.USHORT -> "UInt16"
                UnsignedType.UINT -> "UInt32"
                UnsignedType.ULONG -> "UInt64"
            }
        } ?: if (declaration.isUnit()) "Void" else null

        if (swiftPrimitiveTypeName == null) {
            println("Failed to bridge ${declaration.classFqName}")
        }

        return swiftPrimitiveTypeName?.let { Swift.Type.Nominal("Swift.$it") }
    }
}