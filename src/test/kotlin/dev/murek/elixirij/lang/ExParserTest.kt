package dev.murek.elixirij.lang

import com.intellij.testFramework.ParsingTestCase

class ExParserTest : ParsingTestCase(
    "parser",
    "ex",
    true,
    ExParserDefinition()
) {

    override fun getTestDataPath(): String = "src/test/testData"

    private fun doTest() = doTest(true, true)
    private fun doPartialTest() = doTest(true, false)

    // =============================================================================
    // A. Edge Cases
    // =============================================================================

    fun testEmptyFile() = doTest()
    fun testMultipleExpressions() = doTest()
    fun testTrailingCommaList() = doTest()
    fun testTrailingCommaTuple() = doTest()
    fun testTrailingCommaMap() = doTest()
    
    // =============================================================================
    // 1. Literals
    // =============================================================================

    fun testInteger() = doTest()
    fun testIntegerHex() = doTest()
    fun testIntegerOctal() = doTest()
    fun testIntegerBinary() = doTest()
    fun testIntegerUnderscore() = doTest()
    fun testFloat() = doTest()
    fun testFloatScientific() = doTest()
    fun testChar() = doTest()
    fun testCharEscaped() = doTest()
    fun testString() = doTest()
    fun testStringInterpolation() = doTest()
    fun testStringEscape() = doTest()
    fun testHeredoc() = doTest()
    fun testCharlist() = doTest()
    fun testCharlistHeredoc() = doTest()
    fun testBooleanTrue() = doTest()
    fun testBooleanFalse() = doTest()
    fun testNil() = doTest()

    // =============================================================================
    // 2. Atoms
    // =============================================================================

    fun testAtomSimple() = doTest()
    fun testAtomQuoted() = doTest()
    fun testAtomOperator() = doTest()

    // =============================================================================
    // 3. Identifiers and Aliases
    // =============================================================================

    fun testIdentifierSimple() = doTest()
    fun testIdentifierWithSuffix() = doTest()
    fun testIdentifierWithBang() = doTest()
    fun testIdentifierUnderscore() = doTest()
    fun testAliasSimple() = doTest()
    fun testAliasNested() = doTest()

    // =============================================================================
    // 4. Operators
    // =============================================================================

    fun testOpMatch() = doTest()
    fun testOpPipe() = doTest()
    fun testOpCompare() = doTest()
    fun testOpArithmetic() = doTest()
    fun testOpLogical() = doTest()
    fun testOpIn() = doTest()
    fun testOpRange() = doTest()
    fun testOpRangeStep() = doTest()
    fun testOpConcat() = doTest()
    fun testOpStringConcat() = doTest()
    fun testOpCapture() = doTest()
    fun testOpPin() = doTest()
    fun testOpUnary() = doTest()
    fun testOpAt() = doTest()
    
    // Phase 2: Missing Operators
    fun testOpType() = doTest()
    fun testOpWhen() = doTest()
    fun testOpStab() = doTest()
    fun testOpInMatch() = doTest()
    fun testPipeInContainers() = doTest()

    // =============================================================================
    // 5. Data Structures
    // =============================================================================

    fun testListEmpty() = doTest()
    fun testListSimple() = doTest()
    fun testListKeyword() = doTest()
    fun testListMixed() = doTest()
    fun testListCons() = doTest()
    fun testTupleEmpty() = doTest()
    fun testTupleSimple() = doTest()
    fun testMapEmpty() = doTest()
    fun testMapSimple() = doTest()
    fun testMapArrow() = doTest()
    fun testMapUpdate() = doTest()
    fun testStructSimple() = doTest()
    fun testStructFields() = doTest()
    fun testBitstringEmpty() = doTest()
    fun testBitstringSimple() = doTest()
    fun testBitstringTyped() = doTest()

    // =============================================================================
    // 6. Sigils
    // =============================================================================

    fun testSigilRegex() = doTest()
    fun testSigilString() = doTest()
    fun testSigilWordlist() = doTest()
    fun testSigilCharlist() = doTest()
    fun testSigilHeredoc() = doTest()
    fun testSigilModifiers() = doTest()

    // =============================================================================
    // 7. Parenthesized expressions
    // =============================================================================

    fun testParenExpr() = doTest()
    fun testParenNested() = doTest()

    // =============================================================================
    // 8. Function Calls (Phase 3)
    // =============================================================================

    fun testCallParen() = doTest()
    fun testCallNoArgs() = doTest()
    
    // Phase 3.5: Keyword arguments - requires lexer-level kw_identifier token
    fun testCallKeyword() = doTest()
    fun testCallMixed() = doTest()
    
    fun testCallNested() = doTest()
    fun testCallQualified() = doTest()
    fun testCallQualifiedNested() = doTest()
    fun testCallRemote() = doTest()
    fun testCallAnonymous() = doTest()

    // =============================================================================
    // 9. Anonymous Functions (Phase 5)
    // =============================================================================

    fun testFnSimple() = doTest()
    fun testFnOneArg() = doTest()
    fun testFnMultiArg() = doTest()
    fun testFnMultiClause() = doTest()
    fun testFnMultiClauseNewline() = doTest()
    fun testFnEmptyArgs() = doTest()
    fun testFnPattern() = doTest()
    fun testFnGuard() = doTest()
    fun testFnTypeArgInvalid() = doPartialTest()

    // =============================================================================
    // 10. Capture Expressions (Phase 6)
    // =============================================================================

    fun testCaptureNamed() = doTest()
    fun testCaptureAnonymous() = doTest()
    fun testCaptureQualified() = doTest()
}
