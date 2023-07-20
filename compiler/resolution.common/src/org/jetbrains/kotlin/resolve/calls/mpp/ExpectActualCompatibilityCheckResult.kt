/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.mpp.DeclarationSymbolMarker
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility

internal typealias SymbolsWithCompatibilities<T> = List<Pair<T, ExpectActualCompatibility<T>>>

internal typealias ExpectClassScopeMembersMapping<T> = Map</* expect member */ out T, SymbolsWithCompatibilities<T>>

sealed interface ExpectActualCompatibilityCheckResult<out T : DeclarationSymbolMarker> {
    val compatibility: ExpectActualCompatibility<T>
}

data class ExpectActualCallableCompatibilityCheckResult<out T : DeclarationSymbolMarker> internal constructor(
    override val compatibility: ExpectActualCompatibility<T>,
) : ExpectActualCompatibilityCheckResult<T>

data class ExpectActualClassifierCompatibilityCheckResult<out T : DeclarationSymbolMarker> internal constructor(
    override val compatibility: ExpectActualCompatibility<T>,
    val membersMapping: ExpectClassScopeMembersMapping<T>,
) : ExpectActualCompatibilityCheckResult<T>
