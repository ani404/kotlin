/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinWasmTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinWasmTargetPreset

@KotlinGradlePluginDsl
interface KotlinTargetContainerWithWasmPresetFunctions : KotlinTargetContainerWithPresetFunctions {
    @ExperimentalWasmDsl
    fun wasmJs(
        name: String = "wasmJs",
        configure: KotlinWasmTargetDsl.() -> Unit = { },
    ): KotlinWasmTargetDsl =
        configureOrCreate(
            name,
            presets.getByName("wasm") as KotlinWasmTargetPreset,
            configure
        )

    @ExperimentalWasmDsl
    fun wasmJs() = wasmJs("wasmJs") { }

    @ExperimentalWasmDsl
    fun wasmJs(name: String) = wasmJs(name) { }

    @ExperimentalWasmDsl
    fun wasmJs(name: String, configure: Action<KotlinWasmTargetDsl>) = wasmJs(name) { configure.execute(this) }

    @ExperimentalWasmDsl
    fun wasmJs(configure: Action<KotlinWasmTargetDsl>) = wasmJs { configure.execute(this) }

    @Deprecated("Use wasmJs instead", replaceWith = ReplaceWith("wasmJs(name, configure)"))
    @ExperimentalWasmDsl
    fun wasm(
        name: String = "wasmJs",
        configure: KotlinWasmTargetDsl.() -> Unit = { },
    ): KotlinWasmTargetDsl = wasmJs(name, configure)

    @Deprecated("Use wasmJs instead", replaceWith = ReplaceWith("wasmJs()"))
    @ExperimentalWasmDsl
    fun wasm() = wasmJs()

    @Deprecated("Use wasmJs instead", replaceWith = ReplaceWith("wasmJs(name)"))
    @ExperimentalWasmDsl
    fun wasm(name: String) = wasmJs(name)

    @Deprecated("Use wasmJs instead", replaceWith = ReplaceWith("wasmJs(name, configure)"))
    @ExperimentalWasmDsl
    fun wasm(name: String, configure: Action<KotlinWasmTargetDsl>) = wasmJs(name, configure)

    @Deprecated("Use wasmJs instead", replaceWith = ReplaceWith("wasmJs(configure)"))
    @ExperimentalWasmDsl
    fun wasm(configure: Action<KotlinWasmTargetDsl>) = wasmJs(configure)
}