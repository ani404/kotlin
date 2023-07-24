/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.test.handlers


import org.jetbrains.kotlin.library.KotlinIrSignatureVersion
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.test.model.ArtifactKinds
import org.jetbrains.kotlin.test.model.BinaryArtifactHandler
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension

@OptIn(ExperimentalLibraryAbiReader::class)
class LibraryAbiDumpHandler(testServices: TestServices) : BinaryArtifactHandler<BinaryArtifacts.KLib>(
    testServices,
    ArtifactKinds.KLib,
    failureDisablesNextSteps = true,
    doNotRunIfThereWerePreviousFailures = true,
) {
    private val dumpers = KotlinIrSignatureVersion.CURRENTLY_SUPPORTED_VERSIONS.map { irSignatureVersion ->
        AbiSignatureVersionStub(irSignatureVersion) to MultiModuleInfoDumper()
    }

    override fun processModule(module: TestModule, info: BinaryArtifacts.KLib) {
        val libraryAbi = LibraryAbiReader.readAbiInfo(info.outputFile)

        for ((abiSignatureVersion, dumper) in dumpers) {
            val abiDump = LibraryAbiRenderer.render(libraryAbi, AbiRenderingSettings(abiSignatureVersion))
            dumper.builderForModule(module).append(abiDump)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        assertions.assertAll(
            dumpers.map { (abiSignatureVersion, dumper) ->
                {
                    val expectedFile = testServices
                        .moduleStructure
                        .originalTestDataFiles
                        .first()
                        .withExtension("v${abiSignatureVersion.versionNumber}.txt")
                    assertions.assertEqualsToFile(expectedFile, dumper.generateResultingDump())
                }
            }
        )
    }

    private class AbiSignatureVersionStub(irSignatureVersion: KotlinIrSignatureVersion) : AbiSignatureVersion {
        override val versionNumber = irSignatureVersion.number
        override val isSupportedByAbiReader get() = true
        override val description: String? get() = null
    }
}
