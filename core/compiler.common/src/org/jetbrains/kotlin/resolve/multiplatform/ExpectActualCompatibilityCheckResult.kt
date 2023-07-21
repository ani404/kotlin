/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.multiplatform

import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker

typealias SymbolsWithCompatibilities<T> = List<Pair<T, ExpectActualCompatibilityCheckResult<T>>>

typealias ExpectClassScopeMembersMapping<T> = Map</* expect member */ out T, SymbolsWithCompatibilities<T>>

val SymbolsWithCompatibilities<*>.hasCompatible: Boolean
    get() = any { (_, result) -> result.compatibility.compatible }

sealed interface ExpectActualCompatibilityCheckResult<out T : DeclarationSymbolMarker> : Cloneable {
    val compatibility: ExpectActualCompatibility<T>
}

data class ExpectActualCallableCompatibilityCheckResult<out T : DeclarationSymbolMarker>(
    override val compatibility: ExpectActualCompatibility<T>,
) : ExpectActualCompatibilityCheckResult<T>

data class ExpectActualClassifierCompatibilityCheckResult<out T : DeclarationSymbolMarker>(
    override val compatibility: ExpectActualCompatibility<T>,
    val membersMapping: ExpectClassScopeMembersMapping<T>,
) : ExpectActualCompatibilityCheckResult<T>
