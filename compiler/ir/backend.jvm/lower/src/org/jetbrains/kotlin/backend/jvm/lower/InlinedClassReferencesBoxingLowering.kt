/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.buildSimpleType
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal class InlinedClassReferencesBoxingLowering(val context: JvmBackendContext) : FileLoweringPass, IrElementTransformerVoid() {
    override fun lower(irFile: IrFile) {
        irFile.transformChildrenVoid()
    }

    override fun visitClassReference(expression: IrClassReference): IrExpression {
        return super.visitClassReference(expression).apply {
            val wasTypeParameterClassRefBeforeInline = generateSequence(expression) {
                val originalBeforeInline = it.originalBeforeInline ?: return@generateSequence null
                require(originalBeforeInline is IrClassReference) {
                    "Original for class reference ${it.render()} has another type: ${originalBeforeInline.render()}"
                }
                originalBeforeInline
            }.any { it.classType.classifierOrNull is IrTypeParameterSymbol }

            if (wasTypeParameterClassRefBeforeInline && expression.classType.isPrimitiveType()) {
                require(this is IrClassReference) { "Class reference should preserve its type after transformation" }
                val boxedPrimitive = classType.boxPrimitive(context.ir.symbols)
                classType = boxedPrimitive
                val classReferenceType = type
                require(classReferenceType is IrSimpleType && classReferenceType.isKClass()) {
                    "Type of the type reference is expected to be KClass"
                }
                type = classReferenceType.buildSimpleType {
                    arguments = listOf(boxedPrimitive)
                }
            }

        }
    }

    private fun IrType.boxPrimitive(symbols: JvmSymbols) = when {
        isBoolean() -> symbols.javaLangBool.owner.defaultType
        isByte() -> symbols.javaLangByte.owner.defaultType
        isShort() -> symbols.javaLangShort.owner.defaultType
        isChar() -> symbols.javaLangChar.owner.defaultType
        isInt() -> symbols.javaLangInteger.owner.defaultType
        isFloat() -> symbols.javaLangFloat.owner.defaultType
        isLong() -> symbols.javaLangLong.owner.defaultType
        isDouble() -> symbols.javaLangDouble.owner.defaultType
        else -> throw IllegalArgumentException("Primitive type expected, got ${render()} instead")
    }
}