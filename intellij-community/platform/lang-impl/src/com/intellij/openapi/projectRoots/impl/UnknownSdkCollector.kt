// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.projectRoots.impl

import com.google.common.collect.MultimapBuilder
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager.checkCanceled
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.projectRoots.impl.UnknownSdkResolver.UnknownSdk
import com.intellij.openapi.roots.ModuleJdkOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.concurrency.CancellablePromise
import java.util.*

data class UnknownSdkSnapshot(
  val totallyUnknownSdks: Set<String>,
  val resolvableSdks: List<UnknownSdk>
)

private class MissingSdkInfo(
  private val mySdkName: String,
  private val mySdkType: SdkType
) : UnknownSdk {
  override fun getSdkName() = mySdkName
  override fun getSdkType() = mySdkType
}

class UnknownSdkCollector(private val myProject: Project) {
  fun collectSdksPromise(): CancellablePromise<UnknownSdkSnapshot> {
    return ReadAction.nonBlocking<UnknownSdkSnapshot> { collectSdksUnderReadAction() }
      .expireWith(myProject)
      .coalesceBy(this)
      .submit(AppExecutorUtil.getAppExecutorService())
  }

  private fun collectSdksUnderReadAction(): UnknownSdkSnapshot {

    val sdkToTypes = MultimapBuilder.treeKeys(java.lang.String.CASE_INSENSITIVE_ORDER)
      .hashSetValues()
      .build<String, String>()

    checkCanceled()

    val rootManager = ProjectRootManager.getInstance(myProject)
    if (rootManager.projectSdk == null) {
      val sdkName = rootManager.projectSdkName
      val sdkTypeName = rootManager.projectSdkTypeName

      if (sdkName != null) {
        sdkToTypes.put(sdkName, sdkTypeName)
      }
    }

    for (module in ModuleManager.getInstance(myProject).modules) {
      checkCanceled()

      val moduleRootManager = ModuleRootManager.getInstance(module)
      if (moduleRootManager.externalSource != null) {
        //we do not need to check external modules (e.g. which are imported from Maven or Gradle)
        continue
      }

      val jdkEntry = moduleRootManager.orderEntries
                       .filterIsInstance<ModuleJdkOrderEntry>()
                       .firstOrNull() ?: continue

      if (jdkEntry.jdk == null) {
        val jdkName = jdkEntry.jdkName
        val jdkTypeName = jdkEntry.jdkTypeName

        if (jdkName != null) {
          sdkToTypes.put(jdkName, jdkTypeName)
        }
      }
    }

    val totallyUnknownSdks = TreeSet(String.CASE_INSENSITIVE_ORDER)
    val resolvableSdks = mutableListOf<UnknownSdk>()
    for ((sdkName, sdkTypes) in sdkToTypes.asMap()) {
      val sdkType = sdkTypes.singleOrNull()?.let(SdkType::findByName)
      if (sdkType == null) {
        totallyUnknownSdks.add(sdkName)
      }
      else {
        resolvableSdks.add(MissingSdkInfo(sdkName, sdkType))
      }
    }

    return UnknownSdkSnapshot(totallyUnknownSdks, resolvableSdks)
  }
}
