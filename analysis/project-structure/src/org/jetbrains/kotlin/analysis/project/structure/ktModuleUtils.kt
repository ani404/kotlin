/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.project.structure

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.topologicalSort

/**
 * A list of all modules that the current module can depend on with regular dependency.
 *
 * @see KtModule.directRegularDependencies
 */
public inline fun <reified M : KtModule> KtModule.directRegularDependenciesOfType(): Sequence<M> =
    directRegularDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all modules that the current module can depend on with friend dependency.
 *
 * @see KtModule.directFriendDependencies
 */
public inline fun <reified M : KtModule> KtModule.directFriendDependenciesOfType(): Sequence<M> =
    directFriendDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all modules that the current module can depend on with refinement dependency.
 *
 * @see KtModule.directDependsOnDependencies
 */
public inline fun <reified M : KtModule> KtModule.directDependsOnDependenciesOfType(): Sequence<M> =
    directDependsOnDependencies.asSequence().filterIsInstance<M>()

/**
 * A list of all other modules that the current module can depend on.
 *
 * @see KtModule.directRegularDependencies
 * @see KtModule.directDependsOnDependencies
 * @see KtModule.directFriendDependencies
 */
public fun KtModule.allDirectDependencies(): Sequence<KtModule> =
    sequence {
        yieldAll(directRegularDependencies)
        yieldAll(directDependsOnDependencies)
        yieldAll(directFriendDependencies)
    }

/**
 * A list of all other modules of type [M] that the current module can depend on.
 *
 * @see KtModule.directRegularDependencies
 * @see KtModule.directDependsOnDependencies
 * @see KtModule.directFriendDependencies
 */
public inline fun <reified M : KtModule> KtModule.allDirectDependenciesOfType(): Sequence<M> =
    allDirectDependencies().filterIsInstance<M>()

/**
 * Computes the transitive `dependsOn` dependencies of [directDependsOnDependencies]. [computeTransitiveDependsOnDependencies] is the
 * default computation strategy to provide [KtModule.transitiveDependsOnDependencies].
 *
 * The algorithm is a depth-first search-based topological sort. `dependsOn` dependencies cannot be cyclical and thus form a DAG, which
 * allows the application of a topological sort.
 */
public fun computeTransitiveDependsOnDependencies(directDependsOnDependencies: List<KtModule>): List<KtModule> =
    topologicalSort(directDependsOnDependencies) { this.directDependsOnDependencies }

/**
 * Returns a flat list of all [KtFile] instances from the [roots]
 */
public fun collectKtFiles(roots: List<PsiFileSystemItem>): List<KtFile> {
    val result = mutableListOf<KtFile>()

    fun processDirectory(directory: PsiDirectory) {
        for (file in directory.files) {
            when (file) {
                is KtFile -> result.add(file)
                is PsiDirectory -> processDirectory(file)
            }
        }
    }

    for (file in roots) {
        when (file) {
            is KtFile -> result.add(file)
            is PsiDirectory -> processDirectory(file)
        }
    }

    return result
}
