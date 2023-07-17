/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.platform

sealed class WasmPlatform : SimplePlatform("Wasm") {
    override val oldFashionedDescription: String
        get() = "Wasm "

    override fun toString(): String = targetName
}

object WasmJsPlatform : WasmPlatform() {
    override val targetName: String
        get() = "WasmJs"

    override fun toString(): String = ""
}

object WasmWasiPlatform : WasmPlatform() {
    override val targetName: String
        get() = "WasmWasi"
}

fun TargetPlatform?.isWasm(): Boolean = this?.singleOrNull() is WasmPlatform
fun TargetPlatform?.isWasmJs(): Boolean = this?.singleOrNull() is WasmJsPlatform
fun TargetPlatform?.isWasmWasi(): Boolean = this?.singleOrNull() is WasmWasiPlatform