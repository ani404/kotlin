package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.Task
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.NormalizeLineEndings
import java.io.File

/**
 * A task to incrementally sync a set of files between directories.
 *
 * Incremental sync support greatly reduces task execution time on subsequent builds when a set of files to be synced is large,
 * but actually only a small amount of them is changed.
 */
interface IncrementalSyncTask : Task {

    /**
     * The collection of paths with files to copy.
     *
     * Should be configured using available methods in the [ConfigurableFileCollection]
     * such as [ConfigurableFileCollection.from] or [ConfigurableFileCollection.setFrom].
     *
     * @see [ConfigurableFileCollection]
     */
    @get:InputFiles
    @get:NormalizeLineEndings
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    @get:SkipWhenEmpty
    val from: ConfigurableFileCollection

    /**
     * The directory where the set of files will be copied into.
     */
    @get:OutputDirectory
    val destinationDirectory: Property<File>

    /**
     * @suppress
     */
    @get:Internal
    @Deprecated("Use destinationDirProperty with Provider API", ReplaceWith("destinationDirProperty.get()"))
    val destinationDir: File
        get() = destinationDirectory.get()
}