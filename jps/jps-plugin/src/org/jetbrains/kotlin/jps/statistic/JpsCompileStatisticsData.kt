/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jps.statistic

import org.jetbrains.kotlin.build.report.metrics.BuildAttribute
import org.jetbrains.kotlin.build.report.metrics.JpsBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.JpsBuildTime
import org.jetbrains.kotlin.build.report.statistics.CompileStatisticsData
import org.jetbrains.kotlin.build.report.statistics.StatTag

data class JpsCompileStatisticsData(
    override val projectName: String?,
    override val label: String?,
    override val taskName: String,
    override val taskResult: String?,
    override val startTimeMs: Long,
    override val durationMs: Long,
    override val tags: Set<StatTag>,
    override val changes: List<String>,
    override val buildUuid: String = "Unset",
    override val kotlinVersion: String,
    override val kotlinLanguageVersion: String?,
    override val hostName: String? = "Unset",
    override val finishTime: Long,
    override val compilerArguments: List<String>,
    override val nonIncrementalAttributes: Set<BuildAttribute>,
    override val buildTimesMetrics: Map<JpsBuildTime, Long>,
    override val performanceMetrics: Map<JpsBuildPerformanceMetric, Long>,
    override val gcTimeMetrics: Map<String, Long>?,
    override val gcCountMetrics: Map<String, Long>?,
    override val type: String,
    override val fromKotlinPlugin: Boolean?,
    override val compiledSources: List<String> = emptyList(),
    override val skipMessage: String?,
    override val icLogLines: List<String>,
) : CompileStatisticsData<JpsBuildTime, JpsBuildPerformanceMetric>(
    projectName = projectName, label = label, taskName = taskName, taskResult = taskResult, startTimeMs = startTimeMs,
    durationMs = durationMs, tags = tags, changes = changes, buildUuid = buildUuid, kotlinVersion = kotlinVersion,
    kotlinLanguageVersion = kotlinLanguageVersion, hostName = hostName, finishTime = finishTime,
    compilerArguments = compilerArguments, nonIncrementalAttributes = nonIncrementalAttributes,
    buildTimesMetrics = buildTimesMetrics, performanceMetrics = performanceMetrics, gcTimeMetrics = gcTimeMetrics,
    gcCountMetrics = gcCountMetrics, type = type, fromKotlinPlugin = fromKotlinPlugin, compiledSources = compiledSources,
    skipMessage = skipMessage, icLogLines = icLogLines,
)