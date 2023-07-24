/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.tasks.Nested
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerOptions

/**
 * Represents a Kotlin task performing compilation using configurable [compilerOptions].
 *
 * Check [KotlinCommonCompilerOptions] and inheritors for possible available for configuration compiler options.
 *
 * @see [KotlinCommonCompilerOptions]
 */
interface KotlinCompilationTask<out CO : KotlinCommonCompilerOptions> : Task {

    /**
     * Represents the compiler options used by a Kotlin compilation process with reasonable configured defaults.
     *
     * Could be used to either get the values of currently configured options or to modify them.
     */
    @get:Nested
    val compilerOptions: CO

    /**
     * Configures the [compilerOptions] with the provided configuration.
     */
    fun compilerOptions(configure: CO.() -> Unit) {
        configure(compilerOptions)
    }

    /**
     * Configures the [compilerOptions] with the provided configuration.
     */
    fun compilerOptions(configure: Action<in CO>) {
        configure.execute(compilerOptions)
    }
}
