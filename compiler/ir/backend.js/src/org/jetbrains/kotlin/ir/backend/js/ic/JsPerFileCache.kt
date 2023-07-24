/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.ic

import org.jetbrains.kotlin.backend.common.serialization.cityHash64
import org.jetbrains.kotlin.backend.common.serialization.cityHash64String
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.protobuf.CodedInputStream
import org.jetbrains.kotlin.protobuf.CodedOutputStream
import org.jetbrains.kotlin.utils.addToStdlib.runIf
import java.io.File

class JsPerFileCache(private val moduleArtifacts: List<ModuleArtifact>) : JsMultiArtifactCache<JsPerFileCache.CachedFileInfo>() {
    companion object {
        private const val JS_MODULE_HEADER = "js.module.header.bin"
        private const val CACHED_FILE_JS = "file.js"
        private const val CACHED_EXPORT_FILE_JS = "file.export.js"
        private const val CACHED_FILE_JS_MAP = "file.js.map"
        private const val CACHED_FILE_D_TS = "file.d.ts"
    }

    sealed class CachedFileInfo(val moduleArtifact: ModuleArtifact, moduleHeader: JsIrModuleHeader?) : CacheInfo {
        final override lateinit var jsIrHeader: JsIrModuleHeader

        init {
            if (moduleHeader != null) jsIrHeader = moduleHeader
        }


        sealed class SerializableCachedFileInfo(
            moduleArtifact: ModuleArtifact,
            val fileArtifact: SrcFileArtifact,
            moduleHeader: JsIrModuleHeader?
        ) : CachedFileInfo(moduleArtifact, moduleHeader) {
            var crossFileReferencesHash: ICHash = ICHash()
            fun getArtifactWithName(name: String): File? = moduleArtifact.artifactsDir?.let { File(it, "$filePrefix.$name") }

            private val filePrefix by lazy(LazyThreadSafetyMode.NONE) { fileArtifact.srcFilePath.run { "${substringAfterLast('/')}.${cityHash64()}" } }
        }


        class MainFileCachedInfo(moduleArtifact: ModuleArtifact, fileArtifact: SrcFileArtifact, moduleHeader: JsIrModuleHeader? = null) :
            SerializableCachedFileInfo(moduleArtifact, fileArtifact, moduleHeader) {
            var exportFileCachedInfo: ExportFileCachedInfo? = null

            val jsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_JS) }
            val moduleHeaderArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(JS_MODULE_HEADER) }
            val sourceMapFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_JS_MAP) }
        }

        class ExportFileCachedInfo(
            moduleArtifact: ModuleArtifact,
            fileArtifact: SrcFileArtifact,
            moduleHeader: JsIrModuleHeader? = null,
            var tsDeclarationsHash: Long? = null
        ) : SerializableCachedFileInfo(moduleArtifact, fileArtifact, moduleHeader) {
            val jsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_EXPORT_FILE_JS) }
            val dtsFileArtifact by lazy(LazyThreadSafetyMode.NONE) { getArtifactWithName(CACHED_FILE_D_TS) }
        }

        class ModuleProxyFileCachedInfo(moduleArtifact: ModuleArtifact, moduleHeader: JsIrModuleHeader) : CachedFileInfo(moduleArtifact, moduleHeader)
    }

    private val headerToCachedInfo = hashMapOf<JsIrModuleHeader, CachedFileInfo>()
    private val moduleFragmentToExternalName = ModuleFragmentToExternalName(emptyMap())

    private fun JsIrProgramFragment.getMainFragmentExternalName(moduleArtifact: ModuleArtifact) =
        moduleFragmentToExternalName.getExternalNameFor(name, packageFqn, moduleArtifact.moduleExternalName)

    private fun JsIrProgramFragment.getExportFragmentExternalName(moduleArtifact: ModuleArtifact) =
        moduleFragmentToExternalName.getExternalNameForExporterFile(name, packageFqn, moduleArtifact.moduleExternalName)

    private fun JsIrProgramFragment.asIrModuleHeader(moduleName: String, reexportedIn: String? = null): JsIrModuleHeader {
        return JsIrModuleHeader(
            moduleName = moduleName,
            externalModuleName = moduleName,
            definitions = definitions,
            nameBindings = nameBindings.mapValues { v -> v.value.toString() },
            optionalCrossModuleImports = optionalCrossModuleImports,
            reexportedInModuleWithName = reexportedIn,
            associatedModule = null
        )
    }

    private fun SrcFileArtifact.loadJsIrModuleHeaders(moduleArtifact: ModuleArtifact) = with(loadJsIrFragments()) {
        LoadedJsIrModuleHeaders(
            mainFragment.run { asIrModuleHeader(getMainFragmentExternalName(moduleArtifact)) },
            exportFragment?.run { asIrModuleHeader(mainFragment.getExportFragmentExternalName(moduleArtifact), moduleArtifact.moduleSafeName) },
        )
    }

    private fun <T : CachedFileInfo.SerializableCachedFileInfo> CodedInputStream.loadSingleCachedFileInfo(cachedFileInfo: T): T =
        cachedFileInfo.also {
            val moduleName = readString()
            var reexportedIn: String? = null

            it.crossFileReferencesHash = ICHash.fromProtoStream(this)

            if (it is CachedFileInfo.ExportFileCachedInfo) {
                it.tsDeclarationsHash = runIf(readBool()) { readInt64() }
                reexportedIn = cachedFileInfo.moduleArtifact.moduleSafeName
            }

            val (definitions, nameBindings, optionalCrossModuleImports) = fetchJsIrModuleHeaderNames()

            it.jsIrHeader = JsIrModuleHeader(
                moduleName = moduleName,
                externalModuleName = moduleName,
                definitions = definitions,
                nameBindings = nameBindings,
                optionalCrossModuleImports = optionalCrossModuleImports,
                reexportedInModuleWithName = reexportedIn,
                associatedModule = null,
            )
        }

    private fun <T> CachedFileInfo.MainFileCachedInfo.readModuleHeaderCache(f: CodedInputStream.() -> T): T? =
        moduleHeaderArtifact?.useCodedInputIfExists(f)

    private fun ModuleArtifact.fetchFileInfoFor(fileArtifact: SrcFileArtifact): List<CachedFileInfo>? {
        val moduleArtifact = this
        val mainFileCachedFileInfo = CachedFileInfo.MainFileCachedInfo(moduleArtifact, fileArtifact)

        return mainFileCachedFileInfo.readModuleHeaderCache {
            mainFileCachedFileInfo.run {
                exportFileCachedInfo = fetchFileInfoForExportedPart(this)
                loadSingleCachedFileInfo(this)
                listOfNotNull(exportFileCachedInfo, this)
            }
        }
    }

    private fun CodedInputStream.fetchFileInfoForExportedPart(mainCachedFileInfo: CachedFileInfo.MainFileCachedInfo): CachedFileInfo.ExportFileCachedInfo? {
        return ifTrue {
            loadSingleCachedFileInfo(
                CachedFileInfo.ExportFileCachedInfo(mainCachedFileInfo.moduleArtifact, mainCachedFileInfo.fileArtifact)
            )
        }
    }

    private fun CodedOutputStream.commitSingleFileInfo(cachedFileInfo: CachedFileInfo.SerializableCachedFileInfo) {
        writeStringNoTag(cachedFileInfo.jsIrHeader.externalModuleName)
        cachedFileInfo.crossFileReferencesHash.toProtoStream(this)
        if (cachedFileInfo is CachedFileInfo.ExportFileCachedInfo) {
            ifNotNull(cachedFileInfo.tsDeclarationsHash, ::writeInt64NoTag)
        }
        commitJsIrModuleHeaderNames(cachedFileInfo.jsIrHeader)
    }

    private fun CachedFileInfo.MainFileCachedInfo.commitFileInfo() = moduleHeaderArtifact?.useCodedOutput {
        ifNotNull(exportFileCachedInfo) { commitSingleFileInfo(it) }
        commitSingleFileInfo(this@commitFileInfo)
    }

    private fun ModuleArtifact.generateModuleProxyFileCachedInfo(): CachedFileInfo {
        return CachedFileInfo.ModuleProxyFileCachedInfo(
            this,
            generateProxyIrModuleWith(moduleSafeName, moduleExternalName).makeModuleHeader()
        )
    }

    private fun ModuleArtifact.loadFileInfoFor(fileArtifact: SrcFileArtifact): List<CachedFileInfo> {
        val headers = fileArtifact.loadJsIrModuleHeaders(this)

        val mainCachedFileInfo = CachedFileInfo.MainFileCachedInfo(this, fileArtifact, headers.mainHeader)

        if (headers.exportHeader != null) {
            val tsDeclarationsHash = fileArtifact.loadJsIrFragments().exportFragment?.dts?.raw?.cityHash64()
            val cachedExportFileInfo = mainCachedFileInfo.readModuleHeaderCache { fetchFileInfoForExportedPart(mainCachedFileInfo) }
            mainCachedFileInfo.exportFileCachedInfo = if (cachedExportFileInfo?.tsDeclarationsHash != tsDeclarationsHash) {
                CachedFileInfo.ExportFileCachedInfo(
                    this,
                    fileArtifact,
                    headers.exportHeader,
                    tsDeclarationsHash,
                )
            } else {
                cachedExportFileInfo
            }
        }

        return listOfNotNull(mainCachedFileInfo.exportFileCachedInfo, mainCachedFileInfo)
    }

    private val CachedFileInfo.cachedFiles: CachedFileArtifacts?
        get() = when (this) {
            is CachedFileInfo.MainFileCachedInfo -> jsFileArtifact?.let { CachedFileArtifacts(it, sourceMapFileArtifact, null) }
            is CachedFileInfo.ExportFileCachedInfo -> jsFileArtifact?.let { CachedFileArtifacts(it, null, dtsFileArtifact) }
            is CachedFileInfo.ModuleProxyFileCachedInfo -> null
        }

    override fun getMainModuleAndDependencies(cacheInfo: List<CachedFileInfo>) = null to cacheInfo

    override fun fetchCompiledJsCodeForNullCacheInfo() = PerFileEntryPointCompilationOutput()

    override fun fetchCompiledJsCode(cacheInfo: CachedFileInfo) =
        cacheInfo.cachedFiles?.let { (jsCodeFile, sourceMapFile, tsDeclarationsFile) ->
            jsCodeFile.ifExists { this }
                ?.let { CompilationOutputsCached(it, sourceMapFile?.ifExists { this }, tsDeclarationsFile?.ifExists { this }) }
        }

    override fun commitCompiledJsCode(cacheInfo: CachedFileInfo, compilationOutputs: CompilationOutputsBuilt) =
        cacheInfo.cachedFiles?.let { (jsCodeFile, jsMapFile, tsDeclarationsFile) ->
            tsDeclarationsFile?.writeIfNotNull(compilationOutputs.tsDefinitions?.raw)
            compilationOutputs.writeJsCodeIntoModuleCache(jsCodeFile, jsMapFile)
        } ?: compilationOutputs

    override fun loadJsIrModule(cacheInfo: CachedFileInfo): JsIrModule {
        if (cacheInfo !is CachedFileInfo.SerializableCachedFileInfo) {
            return generateProxyIrModuleWith(cacheInfo.jsIrHeader.moduleName, cacheInfo.jsIrHeader.externalModuleName)
        }

        val fragments = cacheInfo.fileArtifact.loadJsIrFragments()
        val isExportFileCachedInfo = cacheInfo is CachedFileInfo.ExportFileCachedInfo
        return JsIrModule(
            cacheInfo.jsIrHeader.moduleName,
            cacheInfo.jsIrHeader.externalModuleName,
            listOf(if (isExportFileCachedInfo) fragments.exportFragment!! else fragments.mainFragment),
            runIf(isExportFileCachedInfo) { cacheInfo.moduleArtifact.moduleSafeName }
        )
    }

    override fun loadProgramHeadersFromCache(): List<CachedFileInfo> {
        return moduleArtifacts
            .flatMap { moduleArtifact ->
                buildList {
                    var hasFileWithJsExportedDeclaration = false

                    moduleArtifact.fileArtifacts.forEach { srcFileArtifact ->
                        val cachedFileInfo = if (srcFileArtifact.isModified())
                            moduleArtifact.loadFileInfoFor(srcFileArtifact)
                        else
                            moduleArtifact.fetchFileInfoFor(srcFileArtifact) ?: moduleArtifact.loadFileInfoFor(srcFileArtifact)

                        addAll(cachedFileInfo)

                        if (srcFileArtifact.isModified() && cachedFileInfo.any { it is CachedFileInfo.ExportFileCachedInfo }) {
                            hasFileWithJsExportedDeclaration = true
                        }
                    }

                    if (hasFileWithJsExportedDeclaration) add(moduleArtifact.generateModuleProxyFileCachedInfo())
                }
            }
            .onEach { headerToCachedInfo[it.jsIrHeader] = it }
    }

    override fun loadRequiredJsIrModules(crossModuleReferences: Map<JsIrModuleHeader, CrossModuleReferences>) {
        for ((header, references) in crossModuleReferences) {
            val cachedInfo = headerToCachedInfo[header] ?: notFoundIcError("artifact for module ${header.moduleName}")

            if (cachedInfo !is CachedFileInfo.SerializableCachedFileInfo) continue

            val actualCrossModuleHash = references.crossModuleReferencesHashForIC()

            if (header.associatedModule == null && cachedInfo.crossFileReferencesHash != actualCrossModuleHash) {
                header.associatedModule = loadJsIrModule(cachedInfo)
            }

            header.associatedModule?.let {
                cachedInfo.crossFileReferencesHash = actualCrossModuleHash
                if (cachedInfo is CachedFileInfo.MainFileCachedInfo) cachedInfo.commitFileInfo()
            }
        }
    }

    private data class CachedFileArtifacts(val jsCodeFile: File, val sourceMapFile: File?, val tsDeclarationsFile: File?)
    private data class LoadedJsIrModuleHeaders(val mainHeader: JsIrModuleHeader, val exportHeader: JsIrModuleHeader?)
}
