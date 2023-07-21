/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Auto-generated file. DO NOT EDIT!

package kotlin

/**
 * Represents a value which is either `true` or `false`.
 * On the JVM, non-nullable values of this type are represented as values of the primitive type `boolean`.
 */
public class Boolean private constructor() : Comparable<Boolean> {
    @SinceKotlin("1.3")
    companion object {}

    /** Returns the inverse of this boolean. */
    @kotlin.internal.IntrinsicConstEvaluation
    public operator fun not(): Boolean

    /**
     * Performs a logical `and` operation between this Boolean and the [other] one. Unlike the `&&` operator,
     * this function does not perform short-circuit evaluation. Both `this` and [other] will always be evaluated.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public infix fun and(other: Boolean): Boolean

    /**
     * Performs a logical `or` operation between this Boolean and the [other] one. Unlike the `||` operator,
     * this function does not perform short-circuit evaluation. Both `this` and [other] will always be evaluated.
     */
    @kotlin.internal.IntrinsicConstEvaluation
    public infix fun or(other: Boolean): Boolean

    /** Performs a logical `xor` operation between this Boolean and the [other] one. */
    @kotlin.internal.IntrinsicConstEvaluation
    public infix fun xor(other: Boolean): Boolean

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun compareTo(other: Boolean): Int

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun toString(): String

    @kotlin.internal.IntrinsicConstEvaluation
    public override fun equals(other: Any?): Boolean
}
