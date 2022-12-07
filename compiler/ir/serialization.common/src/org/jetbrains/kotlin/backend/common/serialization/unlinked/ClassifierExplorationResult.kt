/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol

/**
 * Describes the reason why a certain classifier is considered as unusable (partially linked or having visibility conflicts).
 * For more details see [LinkedClassifierExplorer.exploreSymbol].
 */
internal sealed interface ClassifierExplorationResult {
    /** Indicated unusable classifier. */
    sealed interface Unusable : ClassifierExplorationResult {
        val symbol: IrClassifierSymbol

        sealed interface CanBeRootCause : Unusable

        /**
         * There is no real owner classifier for the symbol, only synthetic stub created by [MissingDeclarationStubGenerator].
         * Likely the classifier has been deleted in newer version of the library.
         */
        class MissingClassifier(override val symbol: IrClassifierSymbol) : CanBeRootCause

        /**
         * There is no enclosing class for inner class (or enum entry). This might happen if the inner class became a top-level class
         * in newer version of the library.
         */
        class MissingEnclosingClass(override val symbol: IrClassSymbol) : CanBeRootCause

        /**
         * The classifier is effectively inaccessible anywhere in the program because it has conflicting visibility limitations.
         */
        class InaccessibleClassifier(
            override val symbol: IrClassifierSymbol,
            val visibility: ABIVisibility.Limited,
            val classifierWithConflictingLimitation: Usable.AccessibleClassifier
        ) : CanBeRootCause

        /**
         * The classifier is effectively inaccessible anywhere in the program because it has conflicting visibility limitations.
         */
        class InaccessibleClassifierDueToOtherClassifiers(
            override val symbol: IrClassifierSymbol,
            val classifierWithConflictingVisibility1: Usable.AccessibleClassifier,
            val classifierWithConflictingVisibility2: Usable.AccessibleClassifier
        ) : CanBeRootCause

        /**
         * The classifier depends on another partially linked classifier. Thus, it is considered partially linked as well.
         */
        class DueToOtherClassifier(override val symbol: IrClassifierSymbol, val rootCause: CanBeRootCause) : Unusable
    }

    /** Indicates usable type that is fully linked and does not have visibility conflicts. */
    sealed interface Usable : ClassifierExplorationResult {
        val symbol: IrClassifierSymbol
        val visibility: ABIVisibility

        class AccessibleClassifier(override val symbol: IrClassifierSymbol, override val visibility: ABIVisibility) : Usable

        class LesserAccessibleClassifier(override val symbol: IrClassifierSymbol, val dueTo: AccessibleClassifier) : Usable {
            override val visibility = dueTo.visibility
        }
    }

    object RecursionAvoidance : ClassifierExplorationResult
}
