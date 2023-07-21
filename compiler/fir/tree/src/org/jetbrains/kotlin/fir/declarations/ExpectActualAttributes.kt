/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.declarations

import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.resolve.multiplatform.SymbolsWithCompatibilities

private object ExpectForActualAttributeKey : FirDeclarationDataKey()

typealias ExpectForActualData = SymbolsWithCompatibilities<FirBasedSymbol<*>>

@SymbolInternals
var FirDeclaration.expectForActual: ExpectForActualData? by FirDeclarationDataRegistry.data(ExpectForActualAttributeKey)

fun FirFunctionSymbol<*>.getSingleExpectForActualOrNull(): FirFunctionSymbol<*>? =
    (this as FirBasedSymbol<*>).getSingleExpectForActualOrNull() as? FirFunctionSymbol<*>

fun FirBasedSymbol<*>.getSingleExpectForActualOrNull(): FirBasedSymbol<*>? {
    return expectForActual?.singleOrNull()?.first
}

val FirBasedSymbol<*>.expectForActual: ExpectForActualData?
    get() {
        lazyResolveToPhase(FirResolvePhase.EXPECT_ACTUAL_MATCHING)
        return fir.expectForActual
    }

