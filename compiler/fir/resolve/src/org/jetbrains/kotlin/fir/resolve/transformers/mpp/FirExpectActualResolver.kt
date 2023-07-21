/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers.mpp

import org.jetbrains.kotlin.fir.FirExpectActualMatchingContext
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.ExpectForActualData
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.providers.dependenciesSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.createExpectActualTypeParameterSubstitutor
import org.jetbrains.kotlin.mpp.CallableSymbolMarker
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualCompatibilityChecker
import org.jetbrains.kotlin.resolve.multiplatform.compatible

object FirExpectActualResolver {
    private val expectActualCompatibilityChecker = AbstractExpectActualCompatibilityChecker<FirBasedSymbol<*>>()

    fun findExpectForActual(
        actualSymbol: FirBasedSymbol<*>,
        useSiteSession: FirSession,
        scopeSession: ScopeSession,
        context: FirExpectActualMatchingContext,
    ): ExpectForActualData? {
        with(context) {
            val result = when (actualSymbol) {
                is FirCallableSymbol<*> -> {
                    val callableId = actualSymbol.callableId
                    val classId = callableId.classId
                    var parentSubstitutor: ConeSubstitutor? = null
                    var expectContainingClass: FirRegularClassSymbol? = null
                    var actualContainingClass: FirRegularClassSymbol? = null
                    val candidates = when {
                        classId != null -> {
                            expectContainingClass = useSiteSession.dependenciesSymbolProvider.getClassLikeSymbolByClassId(classId)?.let {
                                it.fullyExpandedClass(it.moduleData.session)
                            }
                            actualContainingClass = useSiteSession.symbolProvider.getClassLikeSymbolByClassId(classId)
                                ?.fullyExpandedClass(useSiteSession)

                            val expectTypeParameters = expectContainingClass?.typeParameterSymbols.orEmpty()
                            val actualTypeParameters = actualContainingClass
                                ?.typeParameterSymbols
                                .orEmpty()

                            parentSubstitutor = createExpectActualTypeParameterSubstitutor(
                                expectTypeParameters,
                                actualTypeParameters,
                                useSiteSession,
                            )

                            when (actualSymbol) {
                                is FirConstructorSymbol -> expectContainingClass?.getConstructors(scopeSession)
                                else -> expectContainingClass?.getMembersForExpectClass(actualSymbol.name)
                            }.orEmpty()
                        }
                        callableId.isLocal -> return null
                        else -> {
                            val scope = FirPackageMemberScope(callableId.packageName, useSiteSession, useSiteSession.dependenciesSymbolProvider)
                            mutableListOf<FirCallableSymbol<*>>().apply {
                                scope.processFunctionsByName(callableId.callableName) { add(it) }
                                scope.processPropertiesByName(callableId.callableName) { add(it) }
                            }
                        }
                    }
                    candidates.filter { expectSymbol ->
                        actualSymbol != expectSymbol && expectSymbol.isExpect
                    }.map { expectDeclaration ->
                        expectDeclaration to expectActualCompatibilityChecker.areCompatibleCallables(
                            expectDeclaration,
                            actualSymbol as CallableSymbolMarker,
                            parentSubstitutor,
                            expectContainingClass,
                            actualContainingClass,
                            context
                        )
                    }.let { actualsWithCompatibilities ->
                        // If there is a compatible entry, return a map only containing it
                        val compatibleSymbols = actualsWithCompatibilities.filter { (_, result) -> result.compatibility.compatible }
                        when (compatibleSymbols.isEmpty()) {
                            true -> actualsWithCompatibilities
                            false -> compatibleSymbols
                        }
                    }
                }
                is FirClassLikeSymbol<*> -> {
                    val expectClassSymbol = useSiteSession.dependenciesSymbolProvider
                        .getClassLikeSymbolByClassId(actualSymbol.classId) as? FirRegularClassSymbol ?: return null
                    val result = expectActualCompatibilityChecker.areCompatibleClassifiersAndScopes(
                        expectClassSymbol,
                        actualSymbol,
                        context
                    )
                    listOf(expectClassSymbol to result)
                }
                else -> null
            }
            return result
        }
    }
}
