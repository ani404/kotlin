/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.isTest
import org.jetbrains.kotlin.gradle.plugin.sources.KotlinDependencyScope
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.androidSourceSetInfoOrNull
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.sources.sourceSetDependencyConfigurationByScope
import org.jetbrains.kotlin.gradle.targets.js.npm.SemVer
import org.jetbrains.kotlin.gradle.utils.withType

internal const val KOTLIN_STDLIB_COMMON_MODULE_NAME = "kotlin-stdlib-common"
internal const val KOTLIN_STDLIB_MODULE_NAME = "kotlin-stdlib"
internal const val KOTLIN_STDLIB_JDK7_MODULE_NAME = "kotlin-stdlib-jdk7"
internal const val KOTLIN_STDLIB_JDK8_MODULE_NAME = "kotlin-stdlib-jdk8"
internal const val KOTLIN_STDLIB_JS_MODULE_NAME = "kotlin-stdlib-js"
internal const val KOTLIN_STDLIB_WASM_MODULE_NAME = "kotlin-stdlib-wasm"
internal const val KOTLIN_ANDROID_JVM_STDLIB_MODULE_NAME = KOTLIN_STDLIB_MODULE_NAME

internal fun Project.configureStdlibDefaultDependency(
    topLevelExtension: KotlinTopLevelExtension,
    coreLibrariesVersion: Provider<String>
) {
    when (topLevelExtension) {
        is KotlinJsProjectExtension -> topLevelExtension.registerTargetObserver { target ->
            target?.addStdlibDependency(configurations, dependencies, coreLibrariesVersion)
        }

        is KotlinSingleTargetExtension<*> -> topLevelExtension
            .target
            .addStdlibDependency(configurations, dependencies, coreLibrariesVersion)

        is KotlinMultiplatformExtension -> topLevelExtension
            .targets
            .withType<KotlinMetadataTarget>()
            .configureEach { target ->
                if (target.platformType == KotlinPlatformType.common) {
                    target.addStdlibDependency(configurations, dependencies, coreLibrariesVersion)
                }
            }
    }
}

/**
 * Aligning kotlin-stdlib-jdk8 and kotlin-stdlib-jdk7 dependencies versions with kotlin-stdlib (or kotlin-stdlib-jdk7)
 * when project stdlib version is >= 1.8.0
 */
internal fun ConfigurationContainer.configureStdlibVersionAlignment() = all { configuration ->
    configuration.withDependencies { dependencySet ->
        dependencySet
            .withType<ExternalDependency>()
            .configureEach { dependency ->
                if (dependency.group == KOTLIN_MODULE_GROUP &&
                    (dependency.name == KOTLIN_STDLIB_MODULE_NAME || dependency.name == KOTLIN_STDLIB_JDK7_MODULE_NAME) &&
                    dependency.version != null &&
                    SemVer.fromGradleRichVersion(dependency.version!!) >= kotlin180Version
                ) {
                    if (configuration.isCanBeResolved) configuration.alignStdlibJvmVariantVersions(dependency)

                    // dependency substitution only works for resolvable configuration,
                    // so we need to find all configuration that extends the current one
                    filter {
                        it.isCanBeResolved && it.hierarchy.contains(configuration)
                    }.forEach {
                        it.alignStdlibJvmVariantVersions(dependency)
                    }
                }
            }
    }
}

private fun Configuration.alignStdlibJvmVariantVersions(
    kotlinStdlibDependency: ExternalDependency
) {
    resolutionStrategy.dependencySubstitution {
        if (kotlinStdlibDependency.name != KOTLIN_STDLIB_JDK7_MODULE_NAME) {
            it.substitute(it.module("org.jetbrains.kotlin:kotlin-stdlib-jdk7"))
                .using(it.module("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${kotlinStdlibDependency.version}"))
                .because("kotlin-stdlib-jdk7 is now part of kotlin-stdlib")
        }

        it.substitute(it.module("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
            .using(it.module("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinStdlibDependency.version}"))
            .because("kotlin-stdlib-jdk8 is now part of kotlin-stdlib")
    }
}

private fun KotlinTarget.addStdlibDependency(
    configurations: ConfigurationContainer,
    dependencies: DependencyHandler,
    coreLibrariesVersion: Provider<String>
) {
    compilations.configureEach { compilation ->
        compilation.allKotlinSourceSets.forEach { kotlinSourceSet ->
            val scope = if (compilation.isTest() ||
                (this is KotlinAndroidTarget &&
                        kotlinSourceSet.isRelatedToAndroidTestSourceSet()
                        )
            ) {
                KotlinDependencyScope.IMPLEMENTATION_SCOPE
            } else {
                KotlinDependencyScope.API_SCOPE
            }
            val scopeConfiguration = configurations
                .sourceSetDependencyConfigurationByScope(kotlinSourceSet, scope)

            scopeConfiguration.withDependencies { dependencySet ->
                // Check if stdlib is directly added to SourceSet
                if (isStdlibAddedByUser(configurations, stdlibModules, kotlinSourceSet)) return@withDependencies

                // Check if stdlib module is added to SourceSets hierarchy
                if (
                    isStdlibAddedByUser(
                        configurations,
                        setOf(KOTLIN_STDLIB_MODULE_NAME),
                        *kotlinSourceSet.internal.dependsOnClosure.toTypedArray()
                    )
                ) return@withDependencies

                dependencySet.addLater(
                    coreLibrariesVersion.map {
                        dependencies.kotlinDependency(KOTLIN_STDLIB_MODULE_NAME, it)
                    }
                )
            }
        }
    }
}

internal fun isStdlibAddedByUser(
    configurations: ConfigurationContainer,
    stdlibModules: Set<String>,
    vararg sourceSets: KotlinSourceSet
): Boolean {
    return sourceSets
        .asSequence()
        .flatMap { sourceSet ->
            KotlinDependencyScope.values().map { scope ->
                configurations.sourceSetDependencyConfigurationByScope(sourceSet, scope)
            }.asSequence()
        }
        .flatMap { it.allNonProjectDependencies().asSequence() }
        .any { dependency ->
            dependency.group == KOTLIN_MODULE_GROUP && dependency.name in stdlibModules
        }
}

private val androidTestVariants = setOf(AndroidVariantType.UnitTest, AndroidVariantType.InstrumentedTest)

private val kotlin180Version = SemVer(1.toBigInteger(), 8.toBigInteger(), 0.toBigInteger())

private fun KotlinSourceSet.isRelatedToAndroidTestSourceSet(): Boolean {
    val androidVariant = androidSourceSetInfoOrNull?.androidVariantType ?: return false
    return androidVariant in androidTestVariants
}

internal val stdlibModules = setOf(
    KOTLIN_STDLIB_COMMON_MODULE_NAME,
    KOTLIN_STDLIB_MODULE_NAME,
    KOTLIN_STDLIB_JDK7_MODULE_NAME,
    KOTLIN_STDLIB_JDK8_MODULE_NAME,
    KOTLIN_STDLIB_JS_MODULE_NAME,
    KOTLIN_STDLIB_WASM_MODULE_NAME
)
