/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.formatter;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.File;
import java.util.regex.Pattern;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.test.InnerTestClasses;
import org.jetbrains.jet.test.TestMetadata;

import org.jetbrains.jet.formatter.AbstractJetFormatterTest;

/** This class is generated by {@link org.jetbrains.jet.generators.tests.GenerateTests}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("idea/testData/formatter")
public class JetFormatterTestGenerated extends AbstractJetFormatterTest {
    public void testAllFilesPresentInFormatter() throws Exception {
        JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), "org.jetbrains.jet.generators.tests.GenerateTests", new File("idea/testData/formatter"), Pattern.compile("^([^\\.]+)\\.kt$"), true);
    }
    
    @TestMetadata("BlockFor.kt")
    public void testBlockFor() throws Exception {
        doTest("idea/testData/formatter/BlockFor.kt");
    }
    
    @TestMetadata("Class.kt")
    public void testClass() throws Exception {
        doTest("idea/testData/formatter/Class.kt");
    }
    
    @TestMetadata("CommentInFunctionLiteral.kt")
    public void testCommentInFunctionLiteral() throws Exception {
        doTest("idea/testData/formatter/CommentInFunctionLiteral.kt");
    }
    
    @TestMetadata("ConsecutiveCalls.kt")
    public void testConsecutiveCalls() throws Exception {
        doTest("idea/testData/formatter/ConsecutiveCalls.kt");
    }
    
    @TestMetadata("EmptyLineAfterPackage.kt")
    public void testEmptyLineAfterPackage() throws Exception {
        doTest("idea/testData/formatter/EmptyLineAfterPackage.kt");
    }
    
    @TestMetadata("ForNoBraces.kt")
    public void testForNoBraces() throws Exception {
        doTest("idea/testData/formatter/ForNoBraces.kt");
    }
    
    @TestMetadata("FunctionCallParametersAlign.kt")
    public void testFunctionCallParametersAlign() throws Exception {
        doTest("idea/testData/formatter/FunctionCallParametersAlign.kt");
    }
    
    @TestMetadata("FunctionDefParametersAlign.kt")
    public void testFunctionDefParametersAlign() throws Exception {
        doTest("idea/testData/formatter/FunctionDefParametersAlign.kt");
    }
    
    @TestMetadata("FunctionWithInference.kt")
    public void testFunctionWithInference() throws Exception {
        doTest("idea/testData/formatter/FunctionWithInference.kt");
    }
    
    @TestMetadata("FunctionWithNewLineBrace.kt")
    public void testFunctionWithNewLineBrace() throws Exception {
        doTest("idea/testData/formatter/FunctionWithNewLineBrace.kt");
    }
    
    @TestMetadata("FunctionalType.kt")
    public void testFunctionalType() throws Exception {
        doTest("idea/testData/formatter/FunctionalType.kt");
    }
    
    @TestMetadata("GetterAndSetter.kt")
    public void testGetterAndSetter() throws Exception {
        doTest("idea/testData/formatter/GetterAndSetter.kt");
    }
    
    @TestMetadata("If.kt")
    public void testIf() throws Exception {
        doTest("idea/testData/formatter/If.kt");
    }
    
    @TestMetadata("KDoc.kt")
    public void testKDoc() throws Exception {
        doTest("idea/testData/formatter/KDoc.kt");
    }
    
    @TestMetadata("LambdaArrow.kt")
    public void testLambdaArrow() throws Exception {
        doTest("idea/testData/formatter/LambdaArrow.kt");
    }
    
    @TestMetadata("MultilineFunctionLiteral.kt")
    public void testMultilineFunctionLiteral() throws Exception {
        doTest("idea/testData/formatter/MultilineFunctionLiteral.kt");
    }
    
    @TestMetadata("MultilineFunctionLiteralWithParams.kt")
    public void testMultilineFunctionLiteralWithParams() throws Exception {
        doTest("idea/testData/formatter/MultilineFunctionLiteralWithParams.kt");
    }
    
    @TestMetadata("Parameters.kt")
    public void testParameters() throws Exception {
        doTest("idea/testData/formatter/Parameters.kt");
    }
    
    @TestMetadata("PropertyWithInference.kt")
    public void testPropertyWithInference() throws Exception {
        doTest("idea/testData/formatter/PropertyWithInference.kt");
    }
    
    @TestMetadata("ReferenceExpressionFunctionLiteral.kt")
    public void testReferenceExpressionFunctionLiteral() throws Exception {
        doTest("idea/testData/formatter/ReferenceExpressionFunctionLiteral.kt");
    }
    
    @TestMetadata("RemoveSpacesAroundOperations.kt")
    public void testRemoveSpacesAroundOperations() throws Exception {
        doTest("idea/testData/formatter/RemoveSpacesAroundOperations.kt");
    }
    
    @TestMetadata("RightBracketOnNewLine.kt")
    public void testRightBracketOnNewLine() throws Exception {
        doTest("idea/testData/formatter/RightBracketOnNewLine.kt");
    }
    
    @TestMetadata("SaveSpacesInDocComments.kt")
    public void testSaveSpacesInDocComments() throws Exception {
        doTest("idea/testData/formatter/SaveSpacesInDocComments.kt");
    }
    
    @TestMetadata("SingleLineFunctionLiteral.kt")
    public void testSingleLineFunctionLiteral() throws Exception {
        doTest("idea/testData/formatter/SingleLineFunctionLiteral.kt");
    }
    
    @TestMetadata("SpaceAroundExtendColon.kt")
    public void testSpaceAroundExtendColon() throws Exception {
        doTest("idea/testData/formatter/SpaceAroundExtendColon.kt");
    }
    
    @TestMetadata("SpaceBeforeFunctionLiteral.kt")
    public void testSpaceBeforeFunctionLiteral() throws Exception {
        doTest("idea/testData/formatter/SpaceBeforeFunctionLiteral.kt");
    }
    
    @TestMetadata("SpacesAroundOperations.kt")
    public void testSpacesAroundOperations() throws Exception {
        doTest("idea/testData/formatter/SpacesAroundOperations.kt");
    }
    
    @TestMetadata("SpacesAroundUnaryOperations.kt")
    public void testSpacesAroundUnaryOperations() throws Exception {
        doTest("idea/testData/formatter/SpacesAroundUnaryOperations.kt");
    }
    
    @TestMetadata("UnnecessarySpacesInParametersLists.kt")
    public void testUnnecessarySpacesInParametersLists() throws Exception {
        doTest("idea/testData/formatter/UnnecessarySpacesInParametersLists.kt");
    }
    
    @TestMetadata("When.kt")
    public void testWhen() throws Exception {
        doTest("idea/testData/formatter/When.kt");
    }
    
    @TestMetadata("WhenArrow.kt")
    public void testWhenArrow() throws Exception {
        doTest("idea/testData/formatter/WhenArrow.kt");
    }
    
    @TestMetadata("WhenEntryExpr.kt")
    public void testWhenEntryExpr() throws Exception {
        doTest("idea/testData/formatter/WhenEntryExpr.kt");
    }
    
    @TestMetadata("WhenLinesBeforeLbrace.kt")
    public void testWhenLinesBeforeLbrace() throws Exception {
        doTest("idea/testData/formatter/WhenLinesBeforeLbrace.kt");
    }
    
}
