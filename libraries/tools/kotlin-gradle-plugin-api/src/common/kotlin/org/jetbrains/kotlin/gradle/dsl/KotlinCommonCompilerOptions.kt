// DO NOT EDIT MANUALLY!
// Generated by org/jetbrains/kotlin/generators/arguments/GenerateGradleOptions.kt
// To regenerate run 'generateGradleOptions' task
@file:Suppress("RemoveRedundantQualifierName", "Deprecation", "DuplicatedCode")

package org.jetbrains.kotlin.gradle.dsl

interface KotlinCommonCompilerOptions : org.jetbrains.kotlin.gradle.dsl.KotlinCommonCompilerToolOptions {

    /**
     * Allow using declarations only from the specified version of bundled libraries
     * Possible values: "1.5 (deprecated)", "1.6 (deprecated)", "1.7", "1.8", "1.9", "2.0", "2.1 (experimental)"
     * Default value: null
     */
    @get:org.gradle.api.tasks.Optional
    @get:org.gradle.api.tasks.Input
    val apiVersion: org.gradle.api.provider.Property<org.jetbrains.kotlin.gradle.dsl.KotlinVersion>

    /**
     * Provide source compatibility with the specified version of Kotlin
     * Possible values: "1.5 (deprecated)", "1.6 (deprecated)", "1.7", "1.8", "1.9", "2.0", "2.1 (experimental)"
     * Default value: null
     */
    @get:org.gradle.api.tasks.Optional
    @get:org.gradle.api.tasks.Input
    val languageVersion: org.gradle.api.provider.Property<org.jetbrains.kotlin.gradle.dsl.KotlinVersion>

    /**
     * Enable usages of API that requires opt-in with an opt-in requirement marker with the given fully qualified name
     * Default value: emptyList<String>()
     */
    @get:org.gradle.api.tasks.Input
    val optIn: org.gradle.api.provider.ListProperty<kotlin.String>

    /**
     * Enable progressive compiler mode. In this mode, deprecations and bug fixes for unstable code take effect immediately, instead of going through a graceful migration cycle. Code written in the progressive mode is backward compatible; however, code written in non-progressive mode may cause compilation errors in the progressive mode.
     * Default value: false
     */
    @get:org.gradle.api.tasks.Input
    val progressiveMode: org.gradle.api.provider.Property<kotlin.Boolean>

    /**
     * Compile using experimental K2. K2 is a new compiler pipeline, no compatibility guarantees are yet provided
     * Default value: false
     */
    @Deprecated(message = "Compiler flag -Xuse-k2 is deprecated; please use language version 2.0 instead", level = DeprecationLevel.WARNING)
    @get:org.gradle.api.tasks.Input
    val useK2: org.gradle.api.provider.Property<kotlin.Boolean>
}
