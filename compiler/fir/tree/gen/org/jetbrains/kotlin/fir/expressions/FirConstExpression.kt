/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.expressions

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.fir.visitors.*
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

abstract class FirConstExpression<T> : FirExpression() {
    abstract override val source: KtSourceElement?
    abstract override val typeRef: FirTypeRef
    abstract override val annotations: List<FirAnnotation>
    abstract val kind: ConstantValueKind<T>
    abstract val value: T

    override fun <R, D> accept(visitor: FirVisitor<R, D>, data: D): R = visitor.visitConstExpression(this, data)

    @Suppress("UNCHECKED_CAST")
    override fun <E: FirElement, D> transform(transformer: FirTransformer<D>, data: D): E = 
        transformer.transformConstExpression(this, data) as E

    abstract override fun replaceTypeRef(newTypeRef: FirTypeRef)

    abstract fun replaceKind(newKind: ConstantValueKind<T>)

    abstract override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirConstExpression<T>
}
