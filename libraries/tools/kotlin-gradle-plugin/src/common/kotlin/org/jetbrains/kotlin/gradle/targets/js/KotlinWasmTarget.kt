/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js

import java.util.*

enum class KotlinWasmTarget {
    WASI,
    JS;
}

fun KotlinWasmTarget.toAttribute(): KotlinWasmTargetAttribute {
    return when(this) {
        KotlinWasmTarget.WASI -> KotlinWasmTargetAttribute.wasi
        KotlinWasmTarget.JS -> KotlinWasmTargetAttribute.js
    }
}