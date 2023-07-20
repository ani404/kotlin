/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.work.Incremental
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.dsl.jvm.JvmTargetValidationMode
import org.jetbrains.kotlin.gradle.plugin.CompilerPluginConfig

/**
 * Represent a basic Kotlin task participating in some stage of the build from the provided inputs
 * either by compiling sources or by running some additional Kotlin tool.
 */
interface KotlinCompileTool : PatternFilterable, Task {

    /**
     * Configured task inputs (for example, Kotlin sources) which are used to produce task artifact.
     */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sources: FileCollection

    /**
     * Adds input sources for this task.
     *
     * @param sources object is evaluated as per [org.gradle.api.Project.files].
     */
    fun source(vararg sources: Any)

    /**
     * Sets input sources for this task.
     *
     * **Note**: due to [the bug](https://youtrack.jetbrains.com/issue/KT-59632/KotlinCompileTool.setSource-should-replace-existing-sources)
     * does not replace already added sources.
     *
     * @param sources object is evaluated as per [org.gradle.api.Project.files].
     */
    fun setSource(vararg sources: Any)

    /**
     * Collection of external artifacts participating in the output artifact generation.
     *
     * For example, for a Kotlin/JVM compilation task it will be an external jar files or location with already compiled class files.
     */
    @get:Classpath
    @get:Incremental
    val libraries: ConfigurableFileCollection

    /**
     * The destination directory where produced task artifact will be located.
     */
    @get:OutputDirectory
    val destinationDirectory: DirectoryProperty

    /**
     * @see [PatternFilterable.getExcludes]
     */
    @Internal
    override fun getExcludes(): MutableSet<String>

    /**
     * @see [PatternFilterable.getIncludes]
     */
    @Internal
    override fun getIncludes(): MutableSet<String>
}

/**
 * Represents a base interface for all Kotlin compilation tasks.
 */
interface BaseKotlinCompile : KotlinCompileTool {

    /**
     * Paths to the output directories of the friend modules whose internal declarations should be visible
     */
    @get:Internal
    val friendPaths: ConfigurableFileCollection

    /**
     * Allows adding Kotlin compiler plugins artifacts (usually jar files) to participate in the compilation process.
     */
    @get:Classpath
    val pluginClasspath: ConfigurableFileCollection

    /**
     * Allows adding configuration for Kotlin compiler plugin added into [pluginClasspath] using [CompilerPluginConfig].
     */
    @get:Nested
    val pluginOptions: ListProperty<CompilerPluginConfig>

    // Exists only to be used in 'KotlinCompileCommon' task.
    // Should be removed once 'moduleName' will be moved into CommonCompilerArguments
    /**
     * @suppress
     */
    @get:Input
    val moduleName: Property<String>

    /**
     * Specifies the name of [org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet] task is compiling.
     */
    @get:Internal
    val sourceSetName: Property<String>

    /**
     * Enables [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform-mobile-getting-started.html) flag for compilation process.
     */
    @get:Input
    val multiPlatformEnabled: Property<Boolean>

    /**
     * Enable it in Android projects to use more fine tracking inter-modules incremental compilation.
     */
    @get:Input
    val useModuleDetection: Property<Boolean>
}

/**
 * Represents a Kotlin task compiling given Kotlin sources into JVM class files.
 */
interface KotlinJvmCompile : BaseKotlinCompile,
    KotlinCompileDeprecated<KotlinJvmOptionsDeprecated>,
    KotlinCompilationTask<KotlinJvmCompilerOptions>,
    UsesKotlinJavaToolchain {

    /**
     * @suppress
     */
    @get:Deprecated(
        message = "Please migrate to compilerOptions.moduleName",
        replaceWith = ReplaceWith("compilerOptions.moduleName")
    )
    @get:Optional
    @get:Input
    override val moduleName: Property<String>

    /**
     * @suppress
     */
    // JVM specific
    @get:Internal("Takes part in compiler args.")
    @Deprecated(
        message = "Configure compilerOptions directly",
        replaceWith = ReplaceWith("compilerOptions")
    )
    val parentKotlinOptions: Property<KotlinJvmOptionsDeprecated>

    /**
     * Controls JVM target validation mode between this task and the Java compilation task from Gradle for the same source set.
     *
     * The same JVM targets ensure that the produced jar file contains class files of the same JVM bytecode version,
     * which is important to avoid compatibility issues for the code consumers.
     *
     * Also, Gradle Java compilation task [org.gradle.api.tasks.compile.JavaCompile.targetCompatibility] controls value
     * of "org.gradle.jvm.version" [attribute](https://docs.gradle.org/current/javadoc/org/gradle/api/attributes/java/TargetJvmVersion.html)
     * which itself controls the produced artifact minimal supported JVM version via
     * [Gradle Module Metadata](https://docs.gradle.org/current/userguide/publishing_gradle_module_metadata.html).
     * This allows Gradle to check the compatibility of dependencies at dependency resolution time.
     *
     * To avoid problems with different targets, we advise using [JVM Toolchain](https://kotl.in/gradle/jvm/toolchain) feature.
     *
     * Default value for builds with Gradle <8.0 is [JvmTargetValidationMode.WARNING],
     * while for builds with Gradle 8.0+ it is [JvmTargetValidationMode.ERROR].
     *
     * @since 1.9.0
     */
    @get:Input
    val jvmTargetValidationMode: Property<JvmTargetValidationMode>
}

/**
 * Represent a Kotlin task participating in annotation processing using [Kotlin/Kapt](https://kotlinlang.org/docs/kapt.html).
 *
 * This task generates annotation processing output stubs (without actual methods implementation)
 * using Java source code.
 * These generated stubs then could be referenced in Kotlin source code compilation before doing actual
 * annotation processing.
 */
interface KaptGenerateStubs : KotlinJvmCompile {
    /**
     * The directory where stubs will be generated into.
     */
    @get:OutputDirectory
    val stubsDir: DirectoryProperty

    /**
     * Allows adding artifacts (usually jar files)
     * containing implementation of Java [annotation processor](https://jcp.org/en/jsr/detail?id=269).
     *
     * Should be configured with the same artifacts as in the related [Kapt] task.
     */
    @get:Internal("Not an input, just passed as kapt args. ")
    val kaptClasspath: ConfigurableFileCollection

    /**
     * @suppress
     */
    @get:Deprecated(
        message = "Please migrate to compilerOptions.moduleName",
        replaceWith = ReplaceWith("compilerOptions.moduleName")
    )
    @get:Optional
    @get:Input
    override val moduleName: Property<String>
}

/**
 * Represent a Kotlin task running annotation processing using [Kotlin/Kapt](https://kotlinlang.org/docs/kapt.html).
 *
 * This task should always run after related [KaptGenerateStubs] and [KotlinJvmCompile] tasks in order to work properly.
 */
interface BaseKapt : Task,
    UsesKotlinJavaToolchain {

    // part of kaptClasspath consisting from external artifacts only
    // basically kaptClasspath = kaptExternalClasspath + artifacts built locally
    // TODO (Yahor): should not be a part of public api
    /**
     * @suppress
     */
    @get:Classpath
    val kaptExternalClasspath: ConfigurableFileCollection

    /**
     * Names of Gradle [org.gradle.api.artifacts.Configuration] containing all annotation processor artifacts
     * used to configure [kaptClasspath].
     */
    @get:Internal
    val kaptClasspathConfigurationNames: ListProperty<String>

    /**
     * Output directory containing caches necessary to support incremental annotation processing.
     */
    @get:LocalState
    val incAptCache: DirectoryProperty

    /**
     * Generated by annotation processing class files will be placed in this directory.
     */
    @get:OutputDirectory
    val classesDir: DirectoryProperty

    /**
     * Generated by annotation processing Java source files will be placed in this directory.
     */
    @get:OutputDirectory
    val destinationDir: DirectoryProperty

    // Used in the model builder only
    /**
     * Generated by annotation processing Java source files will be placed in this directory.
     */
    @get:OutputDirectory
    val kotlinSourcesDestinationDir: DirectoryProperty

    /**
     * Represents a list of annotation processor option providers.
     *
     * This property is annotated with @get:Nested to indicate that it should be accessed in a nested manner.
     * The type of each element in the list is Any, meaning it can hold any type of annotation processor option provider.
     */
    @get:Nested
    val annotationProcessorOptionProviders: MutableList<Any>

    /**
     * Directory containing generated by the related [KaptGenerateStubs] task stubs.
     */
    @get:Internal
    val stubsDir: DirectoryProperty

    /**
     * Allows adding artifacts (usually jar files)
     * containing implementation of Java [annotation processor](https://jcp.org/en/jsr/detail?id=269).
     *
     * Should be configured with the same artifacts as in the related [KaptGenerateStubs] task.
     */
    @get:Classpath
    val kaptClasspath: ConfigurableFileCollection

    /**
     * Directory contains compiled by related [KotlinJvmCompile] task classes.
     */
    @get:Internal
    val compiledSources: ConfigurableFileCollection

    /**
     * Contains all artifacts from the related [KotlinJvmCompile.libraries] task input.
     */
    @get:Internal("Task implementation adds correct input annotation.")
    val classpath: ConfigurableFileCollection

    // Needed for the model builder
    /**
     * Specifies the name of [org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet] for which task is
     * doing annotation processing.
     */
    @get:Internal
    val sourceSetName: Property<String>

    /**
     * Contains all Java source code participating in this compilation
     * and generated by related [KaptGenerateStubs] task stubs.
     */
    @get:InputFiles
    @get:IgnoreEmptyDirectories
    @get:Incremental
    @get:NormalizeLineEndings
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val source: ConfigurableFileCollection

    /**
     * Enable searching for annotation processors in the [classpath].
     */
    @get:Input
    val includeCompileClasspath: Property<Boolean>

    /**
     * Configures Java source compatibility for the produced class files and Java source code.
     *
     * @see [org.gradle.api.tasks.compile.AbstractCompile.setSourceCompatibility]
     */
    @get:Internal("Used to compute javac option.")
    val defaultJavaSourceCompatibility: Property<String>
}

/**
 * Represents a [BaseKapt] task whose implementation is running [Kotlin/Kapt](https://kotlinlang.org/docs/kapt.html)
 * directly (without using Kotlin compiler).
 */
interface Kapt : BaseKapt {

    /**
     * Indicates whether JDK classes should be added additionally to the [BaseKapt.classpath].
     *
     * For example, in Android projects this should be disabled.
     */
    @get:Input
    val addJdkClassesToClasspath: Property<Boolean>

    /**
     * File collection contains "org.jetbrains.kotlin:kotlin-annotation-processing-gradle" and "kotlin-stdlib"
     * artifacts which are used to run annotation processing itself.
     *
     * Artifacts' versions should be the same as the version of Kotlin compiler used to compile related Kotlin sources.
     */
    @get:Classpath
    val kaptJars: ConfigurableFileCollection
}