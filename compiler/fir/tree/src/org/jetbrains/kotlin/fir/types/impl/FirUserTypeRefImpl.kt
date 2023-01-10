/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types.impl

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.types.FirQualifierPart
import org.jetbrains.kotlin.fir.types.FirTypeProjection
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.fir.visitors.transformInplace
import org.jetbrains.kotlin.jvm.specialization.annotations.Monomorphic

class FirUserTypeRefImpl(
    override var source: KtSourceElement?,
    override val isMarkedNullable: Boolean,
    override val qualifier: MutableList<FirQualifierPart>,
    override val annotations: MutableList<FirAnnotation>
) : FirUserTypeRef() {
    override val customRenderer: Boolean
        get() = false

    override fun <R, D, @Monomorphic VT : FirVisitor<R, D>> acceptChildren(visitor: VT, data: D) {
        for (part in qualifier) {
            part.typeArgumentList.typeArguments.forEach { it.accept(visitor, data) }
        }
        annotations.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): FirUserTypeRefImpl {
        for (part in qualifier) {
            (part.typeArgumentList.typeArguments as MutableList<FirTypeProjection>).transformInplace(transformer, data)
        }
        transformAnnotations(transformer, data)
        return this
    }

    override fun <D> transformAnnotations(transformer: FirTransformer<D>, data: D): FirUserTypeRef {
        annotations.transformInplace(transformer, data)
        return this
    }
}
