/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.mpp

import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualClassifierCompatibilityCheckResult
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibilityCheckResult
import org.jetbrains.kotlin.resolve.multiplatform.ExpectClassScopeMembersMapping
import org.jetbrains.kotlin.resolve.multiplatform.compatible

object AbstractExpectActualAnnotationMatchChecker {
    private val SKIPPED_CLASS_IDS = setOf(
        StandardClassIds.Annotations.Deprecated,
        StandardClassIds.Annotations.DeprecatedSinceKotlin,
        StandardClassIds.Annotations.OptionalExpectation,
        StandardClassIds.Annotations.RequireKotlin,
        StandardClassIds.Annotations.SinceKotlin,
        StandardClassIds.Annotations.Suppress,
        StandardClassIds.Annotations.WasExperimental,
    )

    class Incompatibility(val expectSymbol: DeclarationSymbolMarker, val actualSymbol: DeclarationSymbolMarker)

    fun areAnnotationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        compatibilityResult: ExpectActualCompatibilityCheckResult<DeclarationSymbolMarker>?,
        checkMemberScope: Boolean,
        context: ExpectActualMatchingContext<*>,
    ): Incompatibility? = with(context) {
        areAnnotationsCompatible(expectSymbol, actualSymbol, compatibilityResult, checkMemberScope)
    }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
        compatibilityResult: ExpectActualCompatibilityCheckResult<*>?,
        checkMemberScope: Boolean,
    ): Incompatibility? {
        return when (expectSymbol) {
            is CallableSymbolMarker -> {
                areCallableAnnotationsCompatible(expectSymbol, actualSymbol as CallableSymbolMarker)
            }
            is RegularClassSymbolMarker -> {
                val membersToCheck = if (checkMemberScope) {
                    (compatibilityResult as ExpectActualClassifierCompatibilityCheckResult).membersMapping
                } else {
                    null
                }
                areClassAnnotationsCompatible(expectSymbol, actualSymbol as ClassLikeSymbolMarker, membersToCheck)
            }
            else -> error("Incorrect types: $expectSymbol $actualSymbol")
        }
    }

    context (ExpectActualMatchingContext<*>)
    private fun areCallableAnnotationsCompatible(
        expectSymbol: CallableSymbolMarker,
        actualSymbol: CallableSymbolMarker,
    ): Incompatibility? {
        commonForClassAndCallableChecks(expectSymbol, actualSymbol)?.let { return it }

        return null
    }

    context (ExpectActualMatchingContext<*>)
    private fun areClassAnnotationsCompatible(
        expectSymbol: RegularClassSymbolMarker,
        actualSymbol: ClassLikeSymbolMarker,
        membersToCheck: ExpectClassScopeMembersMapping<DeclarationSymbolMarker>?,
    ): Incompatibility? {
        if (actualSymbol is TypeAliasSymbolMarker) {
            val expanded = actualSymbol.expandToRegularClass() ?: return null
            return areClassAnnotationsCompatible(expectSymbol, expanded, membersToCheck)
        }
        commonForClassAndCallableChecks(expectSymbol, actualSymbol)?.let { return it }
        if (membersToCheck != null) {
            checkAnnotationsInClassMemberScope(membersToCheck)?.let { return it }
        }
        // TODO(Roman.Efremov, KT-58551): check annotations on fake overrides in case of implicit actualization

        return null
    }

    context (ExpectActualMatchingContext<*>)
    private fun commonForClassAndCallableChecks(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
    ): Incompatibility? {
        areAnnotationsSetOnDeclarationsCompatible(expectSymbol, actualSymbol)?.let { return it }

        return null
    }

    context (ExpectActualMatchingContext<*>)
    private fun areAnnotationsSetOnDeclarationsCompatible(
        expectSymbol: DeclarationSymbolMarker,
        actualSymbol: DeclarationSymbolMarker,
    ): Incompatibility? {
        // TODO(Roman.Efremov, KT-58551): properly handle repeatable annotations
        // TODO(Roman.Efremov, KT-58551): check other annotation targets (constructors, types, value parameters, etc)

        val skipSourceAnnotations = !actualSymbol.hasSourceAnnotationsErased
        val actualAnnotationsByName = actualSymbol.annotations.groupBy { it.classId }

        for (expectAnnotation in expectSymbol.annotations) {
            val expectClassId = expectAnnotation.classId ?: continue
            if (expectClassId in SKIPPED_CLASS_IDS || expectAnnotation.isOptIn) {
                continue
            }
            if (expectAnnotation.isRetentionSource && skipSourceAnnotations) {
                continue
            }
            val actualAnnotationsWithSameClassId = actualAnnotationsByName[expectClassId] ?: emptyList()
            val collectionCompatibilityChecker = getAnnotationCollectionArgumentsCompatibilityChecker(expectClassId)
            if (actualAnnotationsWithSameClassId.none {
                    areAnnotationArgumentsEqual(expectAnnotation, it, collectionCompatibilityChecker)
                }) {
                return Incompatibility(expectSymbol, actualSymbol)
            }
        }
        return null
    }

    private fun getAnnotationCollectionArgumentsCompatibilityChecker(annotationClassId: ClassId):
            ExpectActualCollectionArgumentsCompatibilityCheckStrategy {
        return if (annotationClassId == StandardClassIds.Annotations.Target) {
            ExpectActualCollectionArgumentsCompatibilityCheckStrategy.ExpectIsSubsetOfActual
        } else {
            ExpectActualCollectionArgumentsCompatibilityCheckStrategy.Default
        }
    }

    context (ExpectActualMatchingContext<*>)
    private fun checkAnnotationsInClassMemberScope(
        membersToCheck: ExpectClassScopeMembersMapping<DeclarationSymbolMarker>,
    ): Incompatibility? {
        for ((expectSymbol, actualsWithCompatibilities) in membersToCheck.entries) {
            val (actualSymbol, compatibilityResult) =
                actualsWithCompatibilities.singleOrNull { (_, result) -> result.compatibility.compatible }
                    ?: continue

            areAnnotationsCompatible(expectSymbol, actualSymbol, compatibilityResult, checkMemberScope = true)
                ?.let { return it }
        }
        return null
    }
}