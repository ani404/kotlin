/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirNameConflictsTracker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirOuterClassTypeParameterRef
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl.Companion.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl.Companion.DEFAULT_STATUS_FOR_SUSPEND_MAIN_FUNCTION
import org.jetbrains.kotlin.fir.declarations.impl.modifiersRepresentation
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.scopes.impl.FirPackageMemberScope
import org.jetbrains.kotlin.fir.scopes.impl.toConeType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.lazyResolveToPhase
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.util.ListMultimap
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.utils.SmartSet

val DEFAULT_STATUS_FOR_NORMAL_MAIN_FUNCTION = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS

private val FirSimpleFunction.hasMainFunctionStatus
    get() = when (status.modifiersRepresentation) {
        DEFAULT_STATUS_FOR_NORMAL_MAIN_FUNCTION.modifiersRepresentation,
        DEFAULT_STATUS_FOR_SUSPEND_MAIN_FUNCTION.modifiersRepresentation,
        -> true
        else -> false
    }

private val CallableId.isTopLevel get() = className == null

// - see testEnumValuesValueOf.
// it generates a static function that has
// the same signature as the function defined
// explicitly.
// - see tests with `fun () {}`.
// you can't redeclare something that has no name.
private fun FirDeclaration.isCollectable(): Boolean {
    if (this is FirCallableDeclaration) {
        if (contextReceivers.any { it.typeRef.coneType.hasError() }) return false
        if (typeParameters.any { it.toConeType().hasError() }) return false
        if (receiverParameter?.typeRef?.coneType?.hasError() == true) return false
        if (this is FirFunction && valueParameters.any { it.returnTypeRef.coneType.hasError() }) return false
    }

    return when (this) {
        is FirSimpleFunction -> source?.kind !is KtFakeSourceElementKind && name != SpecialNames.NO_NAME_PROVIDED
        is FirProperty -> source?.kind !is KtFakeSourceElementKind.EnumGeneratedDeclaration
        is FirRegularClass -> name != SpecialNames.NO_NAME_PROVIDED
        // class delegation field will be renamed after by the IR backend in a case of a name clash
        is FirField -> source?.kind != KtFakeSourceElementKind.ClassDelegationField
        else -> true
    }
}

private fun isExpectAndActual(declaration1: FirDeclaration, declaration2: FirDeclaration): Boolean {
    if (declaration1 !is FirMemberDeclaration) return false
    if (declaration2 !is FirMemberDeclaration) return false
    return (declaration1.status.isExpect && declaration2.status.isActual) ||
            (declaration1.status.isActual && declaration2.status.isExpect)
}

private class DeclarationGroup {
    val simpleFunctions = mutableListOf<Pair<FirSimpleFunction, String>>()
    val constructors = mutableListOf<Pair<FirConstructor, String>>()
    val classLikes = mutableListOf<Pair<FirClassLikeDeclaration, String>>()
    val properties = mutableListOf<Pair<FirProperty, String>>()
    val extensionProperties = mutableListOf<Pair<FirProperty, String>>()
}

private fun groupTopLevelByName(declarations: List<FirDeclaration>): Map<Name, DeclarationGroup> {
    val groups = mutableMapOf<Name, DeclarationGroup>()
    for (declaration in declarations) {
        if (!declaration.isCollectable()) continue

        when (declaration) {
            is FirSimpleFunction ->
                groups.getOrPut(declaration.name, ::DeclarationGroup).simpleFunctions +=
                    declaration to FirRedeclarationPresenter.represent(declaration)
            is FirProperty -> {
                val group = groups.getOrPut(declaration.name, ::DeclarationGroup)
                val representation = FirRedeclarationPresenter.represent(declaration)
                if (declaration.receiverParameter != null) {
                    group.extensionProperties += declaration to representation
                } else {
                    group.properties += declaration to representation
                }
            }
            is FirRegularClass -> {
                val group = groups.getOrPut(declaration.name, ::DeclarationGroup)
                group.classLikes += declaration to FirRedeclarationPresenter.represent(declaration)
                if (declaration.classKind != ClassKind.OBJECT) {
                    declaration.declarations
                        .filterIsInstance<FirConstructor>()
                        .mapTo(group.constructors) { it to FirRedeclarationPresenter.represent(it, declaration) }
                }
            }
            is FirTypeAlias ->
                groups.getOrPut(declaration.name, ::DeclarationGroup).classLikes +=
                    declaration to FirRedeclarationPresenter.represent(declaration)
            else -> {}
        }
    }
    return groups
}

/**
 * Collects FirDeclarations for further analysis.
 */
class FirDeclarationInspector(
    private val session: FirSession,
) {
    val declarationConflictingSymbols: HashMap<FirDeclaration, SmartSet<FirBasedSymbol<*>>> = hashMapOf()

    fun collectClassMembers(klass: FirRegularClass) {
        val otherDeclarations = mutableMapOf<String, MutableList<FirDeclaration>>()
        val functionDeclarations = mutableMapOf<String, MutableList<FirDeclaration>>()

        for (it in klass.declarations) {
            if (!it.isCollectable()) continue

            when (it) {
                is FirSimpleFunction -> collect(it, FirRedeclarationPresenter.represent(it), functionDeclarations)
                is FirRegularClass -> collect(it, FirRedeclarationPresenter.represent(it), otherDeclarations)
                is FirTypeAlias -> collect(it, FirRedeclarationPresenter.represent(it), otherDeclarations)
                is FirVariable -> collect(it, FirRedeclarationPresenter.represent(it), otherDeclarations)
                else -> {}
            }
        }
    }

    private fun collect(declaration: FirDeclaration, representation: String, map: MutableMap<String, MutableList<FirDeclaration>>) {
        map.getOrPut(representation, ::mutableListOf).also {
            it.add(declaration)

            val conflicts = SmartSet.create<FirBasedSymbol<*>>()
            for (otherDeclaration in it) {
                if (otherDeclaration != declaration && !isOverloadable(declaration, otherDeclaration)) {
                    conflicts.add(otherDeclaration.symbol)
                    declarationConflictingSymbols.getOrPut(otherDeclaration) { SmartSet.create() }.add(declaration.symbol)
                }
            }

            declarationConflictingSymbols[declaration] = conflicts
        }
    }

    fun collectTopLevel(file: FirFile, packageMemberScope: FirPackageMemberScope) {
        // To check top-level declarations, we iterate the package member scope which will return both declarations in the same file and
        // in different files, i.e., we don't need to explicitly compare declarations in the given file with each other (except classifiers
        // because the scope will only return one class/object/typealias with the given name).

        // We don't want to iterate the scope multiple times for the same name, so we group the declarations in the given file by name.
        // Furthermore, we subdivide the declarations into different types.
        // This allows us to skip iterating certain kinds of declarations (functions, properties, classifiers) because some combinations
        // of declaration kinds can never clash (e.g., functions and properties).

        for ((declarationName, group) in groupTopLevelByName(file.declarations)) {
            fun collectTopLevelConflicts(
                declarations: List<Pair<FirDeclaration, String>>,
                conflictingSymbol: FirBasedSymbol<*>,
                conflictingPresentation: String? = null,
                conflictingFile: FirFile? = null,
            ) {
                for ((declaration, declarationPresentation) in declarations) {
                    collectTopLevelConflict(
                        declaration,
                        declarationPresentation,
                        file,
                        conflictingSymbol,
                        conflictingPresentation,
                        conflictingFile
                    )

                    session.lookupTracker?.recordLookup(declarationName, file.packageFqName.asString(), declaration.source, file.source)
                }
            }

            val groupHasClassLikesOrProperties = group.classLikes.isNotEmpty() || group.properties.isNotEmpty()
            val groupHasSimpleFunctions = group.simpleFunctions.isNotEmpty()

            // Functions can clash with functions and constructors
            if (groupHasSimpleFunctions || group.constructors.isNotEmpty()) {
                packageMemberScope.processFunctionsByName(declarationName) {
                    collectTopLevelConflicts(group.simpleFunctions, it)
                    collectTopLevelConflicts(group.constructors, it)
                }
            }

            // Classes/Type aliases can clash with classes/type aliases and non-extension properties
            if (groupHasClassLikesOrProperties || groupHasSimpleFunctions) {
                packageMemberScope.processClassifiersByNameWithSubstitution(declarationName) { symbol, _ ->
                    collectTopLevelConflicts(group.classLikes, conflictingSymbol = symbol)
                    collectTopLevelConflicts(group.properties, conflictingSymbol = symbol)

                    // Constructors can only clash with functions (we don't want to check constructors clashing with constructors because
                    // we already report redeclaration for the class itself).
                    if (groupHasSimpleFunctions) {
                        if (symbol !is FirRegularClassSymbol) return@processClassifiersByNameWithSubstitution
                        if (symbol.classKind == ClassKind.OBJECT || symbol.classKind == ClassKind.ENUM_ENTRY) return@processClassifiersByNameWithSubstitution

                        symbol.lazyResolveToPhase(FirResolvePhase.STATUS)
                        @OptIn(SymbolInternals::class)
                        val classWithSameName = symbol.fir
                        classWithSameName.declarations.filterIsInstance<FirConstructor>()
                            .forEach { constructor ->
                                collectTopLevelConflicts(
                                    group.simpleFunctions,
                                    conflictingSymbol = constructor.symbol,
                                    conflictingPresentation = FirRedeclarationPresenter.represent(constructor, classWithSameName),
                                )
                            }
                    }
                }
            }

            if (groupHasClassLikesOrProperties) {
                session.nameConflictsTracker?.let { it as? FirNameConflictsTracker }
                    ?.redeclaredClassifiers?.get(ClassId(file.packageFqName, declarationName))?.forEach {
                        collectTopLevelConflicts(group.classLikes, conflictingSymbol = it.classifier, conflictingFile = it.file)
                        collectTopLevelConflicts(group.properties, conflictingSymbol = it.classifier, conflictingFile = it.file)
                    }

                // Check classes/objects/typealiases from the same file because in case of redaclarations, the scope will only return
                // one and the nameConflictsTracker does not seem to work for LL API.
                for ((classLike, representation) in group.classLikes) {
                    collectTopLevelConflicts(
                        group.classLikes,
                        conflictingSymbol = classLike.symbol,
                        conflictingPresentation = representation,
                        conflictingFile = file
                    )
                    collectTopLevelConflicts(
                        group.properties,
                        conflictingSymbol = classLike.symbol,
                        conflictingPresentation = representation,
                        conflictingFile = file
                    )
                }
            }

            // Properties can clash with classes/typealiases and properties
            if (groupHasClassLikesOrProperties || group.extensionProperties.isNotEmpty()) {
                packageMemberScope.processPropertiesByName(declarationName) {
                    collectTopLevelConflicts(group.classLikes, conflictingSymbol = it)
                    collectTopLevelConflicts(group.properties, conflictingSymbol = it)
                    collectTopLevelConflicts(group.extensionProperties, conflictingSymbol = it)
                }
            }
        }
    }

    private fun collectTopLevelConflict(
        declaration: FirDeclaration,
        declarationPresentation: String,
        containingFile: FirFile,
        conflictingSymbol: FirBasedSymbol<*>,
        conflictingPresentation: String? = null,
        conflictingFile: FirFile? = null,
    ) {
        conflictingSymbol.lazyResolveToPhase(FirResolvePhase.STATUS)
        @OptIn(SymbolInternals::class)
        val conflicting = conflictingSymbol.fir
        if (conflicting == declaration || declaration.moduleData != conflicting.moduleData) return
        val actualConflictingPresentation = conflictingPresentation ?: FirRedeclarationPresenter.represent(conflicting)
        if (actualConflictingPresentation != declarationPresentation) return
        val actualConflictingFile =
            conflictingFile ?: when (conflictingSymbol) {
                is FirClassLikeSymbol<*> -> session.firProvider.getFirClassifierContainerFileIfAny(conflictingSymbol)
                is FirCallableSymbol<*> -> session.firProvider.getFirCallableContainerFile(conflictingSymbol)
                else -> null
            }
        if (containingFile == actualConflictingFile && conflicting.origin == FirDeclarationOrigin.Precompiled) {
            return // TODO: rewrite local decls checker to the same logic and then remove the check
        }
        if (!conflicting.isCollectable()) return
        if (areCompatibleMainFunctions(declaration, containingFile, conflicting, actualConflictingFile)) return
        if (
            conflicting is FirMemberDeclaration &&
            !session.visibilityChecker.isVisible(conflicting, session, containingFile, emptyList(), dispatchReceiver = null)
        ) return
        if (isOverloadable(declaration, conflicting)) return

        declarationConflictingSymbols.getOrPut(declaration) { SmartSet.create() }.add(conflictingSymbol)
    }

    private fun FirSimpleFunction.representsMainFunctionAllowingConflictingOverloads(): Boolean {
        if (name != StandardNames.MAIN || !symbol.callableId.isTopLevel || !hasMainFunctionStatus) return false
        if (receiverParameter != null || typeParameters.isNotEmpty()) return false
        if (valueParameters.isEmpty()) return true
        val paramType = valueParameters.singleOrNull()?.returnTypeRef?.coneType?.fullyExpandedType(session) ?: return false
        if (!paramType.isNonPrimitiveArray) return false
        return paramType.typeArguments.singleOrNull()?.type?.fullyExpandedType(session)?.isString == true
    }

    private fun areCompatibleMainFunctions(
        declaration1: FirDeclaration, file1: FirFile, declaration2: FirDeclaration, file2: FirFile?,
    ) = file1 != file2
            && declaration1 is FirSimpleFunction
            && declaration2 is FirSimpleFunction
            && declaration1.representsMainFunctionAllowingConflictingOverloads()
            && declaration2.representsMainFunctionAllowingConflictingOverloads()

    private fun isOverloadable(
        declaration: FirDeclaration,
        conflicting: FirDeclaration,
    ): Boolean {
        if (isExpectAndActual(declaration, conflicting)) return true

        val declarationIsLowPriority = hasLowPriorityAnnotation(declaration.annotations)
        val conflictingIsLowPriority = hasLowPriorityAnnotation(conflicting.annotations)
        if (declarationIsLowPriority != conflictingIsLowPriority) return true

        return declaration is FirCallableDeclaration &&
                conflicting is FirCallableDeclaration &&
                session.declarationOverloadabilityHelper.isOverloadable(declaration, conflicting)
    }

}

/** Checks for redeclarations of value and type parameters, and local variables. */
fun checkForLocalRedeclarations(elements: List<FirElement>, context: CheckerContext, reporter: DiagnosticReporter) {
    if (elements.size <= 1) return

    val multimap = ListMultimap<Name, FirBasedSymbol<*>>()

    for (element in elements) {
        val name: Name?
        val symbol: FirBasedSymbol<*>?
        when (element) {
            is FirVariable -> {
                symbol = element.symbol
                name = element.name
            }
            is FirOuterClassTypeParameterRef -> {
                continue
            }
            is FirTypeParameterRef -> {
                symbol = element.symbol
                name = symbol.name
            }
            else -> {
                symbol = null
                name = null
            }
        }
        if (name?.isSpecial == false) {
            multimap.put(name, symbol!!)
        }
    }
    for (key in multimap.keys) {
        val conflictingElements = multimap[key]
        if (conflictingElements.size > 1) {
            for (conflictingElement in conflictingElements) {
                reporter.reportOn(conflictingElement.source, FirErrors.REDECLARATION, conflictingElements, context)
            }
        }
    }
}
