/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility

internal typealias SymbolsWithCompatibilities<T> = List<Pair<T, ExpectActualCompatibility<T>>>

internal typealias ExpectClassScopeMembersMapping<T> = Map</* expect member */ T, SymbolsWithCompatibilities<T>>
