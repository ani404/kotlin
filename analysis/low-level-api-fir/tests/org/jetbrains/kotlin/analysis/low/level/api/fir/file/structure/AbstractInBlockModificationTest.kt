/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure

import com.intellij.extapi.psi.ASTDelegatePsiElement
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.getOrBuildFirOfType
import org.jetbrains.kotlin.analysis.low.level.api.fir.lazyResolveRenderer
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolveWithCaches
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirResolvableModuleSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.base.AbstractLowLevelApiSingleFileTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirOutOfContentRootTestConfigurator
import org.jetbrains.kotlin.analysis.low.level.api.fir.test.configurators.AnalysisApiFirSourceTestConfigurator
import org.jetbrains.kotlin.analysis.test.framework.services.expressionMarkerProvider
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirPropertyAccessor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.test.services.TestModuleStructure
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions

abstract class AbstractInBlockModificationTest : AbstractLowLevelApiSingleFileTest() {
    override fun doTestByFileStructure(ktFile: KtFile, moduleStructure: TestModuleStructure, testServices: TestServices) {
        val selectedElement = testServices.expressionMarkerProvider.getSelectedElementOfTypeByDirective(
            ktFile = ktFile,
            module = moduleStructure.modules.last(),
        )

        val declaration = selectedElement.getNonLocalReanalyzableContainingDeclaration()
        if (declaration == null) {
            testServices.assertions.assertEqualsToTestDataFileSibling("IN-BLOCK MODIFICATION IS NOT APPLICABLE FOR THIS PLACE")
        } else {
            doTest(declaration, ktFile, testServices, inplaceInvalidation = true)
            doTest(declaration, ktFile, testServices, inplaceInvalidation = false)
        }
    }

    private fun doTest(declaration: KtDeclaration, ktFile: KtFile, testServices: TestServices, inplaceInvalidation: Boolean) {
        val actual = resolveWithCaches(ktFile) { firSession ->
            val firDeclarationBefore = declaration.getOrBuildFirOfType<FirDeclaration>(firSession)
            val declarationTextBefore = firDeclarationBefore.render()

            if (!inplaceInvalidation) {
                declaration.modifyBody()
                invalidateAfterInBlockModification(declaration)
            } else {
                val module = firSession.getModule(ktFile)
                val session = firSession.getSessionFor(module) as LLFirResolvableModuleSession
                val structureElement = FileStructure.build(ktFile, session.moduleComponents).getStructureElementFor(declaration)
                // we should get a structure element before modification to avoid reanalyzing
                declaration.modifyBody()
                (structureElement as ReanalyzableStructureElement<*, *>).inPlaceInBlockInvalidation()
            }

            val declarationTextAfterModification = firDeclarationBefore.render()
            testServices.assertions.assertNotEquals(declarationTextBefore, declarationTextAfterModification) {
                "The declaration before and after modification must be in different state"
            }

            val firDeclarationAfter = declaration.getOrBuildFirOfType<FirDeclaration>(firSession)
            testServices.assertions.assertEquals(firDeclarationBefore, firDeclarationAfter) {
                "The declaration before and after must be the same"
            }

            val declarationTextAfter = firDeclarationAfter.render()
            testServices.assertions.assertEquals(declarationTextBefore, declarationTextAfter) {
                "The declaration must have the same in the resolved state"
            }

            "BEFORE MODIFICATION:\n$declarationTextBefore\nAFTER MODIFICATION:\n$declarationTextAfterModification"
        }

        testServices.assertions.assertEqualsToTestDataFileSibling(
            actual = actual,
            testPrefix = if (inplaceInvalidation)
                "inplace" + configurator.testPrefix?.let { ".$it" }.orEmpty()
            else
                configurator.testPrefix,
        )
    }

    /**
     * Emulate modification inside the body
     */
    private fun KtDeclaration.modifyBody() {
        parentsWithSelf.filterIsInstance<ASTDelegatePsiElement>().forEach {
            it.subtreeChanged()
        }
    }
}

private fun FirDeclaration.render(): String {
    val declarationToRender = if (this is FirPropertyAccessor) propertySymbol.fir else this
    return lazyResolveRenderer(StringBuilder()).renderElementAsString(declarationToRender)
}

abstract class AbstractSourceInBlockModificationTest : AbstractInBlockModificationTest() {
    override val configurator = AnalysisApiFirSourceTestConfigurator(analyseInDependentSession = false)
}

abstract class AbstractOutOfContentRootInBlockModificationTest : AbstractInBlockModificationTest() {
    override val configurator = AnalysisApiFirOutOfContentRootTestConfigurator
}