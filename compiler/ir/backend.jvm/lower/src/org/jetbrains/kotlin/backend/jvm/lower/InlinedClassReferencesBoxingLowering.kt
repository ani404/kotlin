/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.getAttributeOwnerBeforeInline
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
        val transformedReference = super.visitClassReference(expression)

        require(transformedReference is IrClassReference) { "Class reference should preserve its type after transformation" }

        val wasTypeParameterClassRefBeforeInline =
            (expression.getAttributeOwnerBeforeInline() as? IrClassReference)?.classType?.classifierOrNull is IrTypeParameterSymbol

        if (wasTypeParameterClassRefBeforeInline && transformedReference.classType.isPrimitiveType()) {
            val boxedPrimitive = transformedReference.classType.boxPrimitive()
            transformedReference.classType = boxedPrimitive
            val classReferenceType = transformedReference.type
            require(classReferenceType is IrSimpleType && classReferenceType.isKClass()) {
                "Type of the type reference is expected to be KClass"
            }
            transformedReference.type = classReferenceType.buildSimpleType {
                arguments = listOf(boxedPrimitive)
            }
        }
        return transformedReference
    }

    private fun IrType.boxPrimitive() = with(context.ir.symbols) {
        when {
            isBoolean() -> javaLangBool.owner.defaultType
            isByte() -> javaLangByte.owner.defaultType
            isShort() -> javaLangShort.owner.defaultType
            isChar() -> javaLangChar.owner.defaultType
            isInt() -> javaLangInteger.owner.defaultType
            isFloat() -> javaLangFloat.owner.defaultType
            isLong() -> javaLangLong.owner.defaultType
            isDouble() -> javaLangDouble.owner.defaultType
            else -> throw IllegalArgumentException("Primitive type expected, got ${render()} instead")
        }
    }
}