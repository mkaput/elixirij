package dev.murek.elixirij.lang.parser

import com.intellij.testFramework.ParsingTestCase
import dev.murek.elixirij.lang.ExParserDefinition

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
    fun testOnlyWhitespace() = doTest()
    fun testOnlyComments() = doTest()
    fun testTrailingComma() = doTest()
    fun testNewlinesInExpr() = doTest()
    fun testNewlinesAroundOp() = doTest()
    fun testMultipleStatements() = doTest()
    fun testNestedDoBlocks() = doTest()
    fun testOperatorAsAtom() = doTest()
    fun testMultipleExpressions() = doTest()
    fun testTrailingCommaList() = doTest()
    fun testTrailingCommaTuple() = doTest()
    fun testTrailingCommaMap() = doTest()

    // =============================================================================
    // B. Past Bugs
    // =============================================================================

    fun testUseKeywordListAllowsNewlines() = doTest()
    fun testStructNewlineFields() = doTest()

    // =============================================================================
    // C. Parser Grilling
    // =============================================================================

    fun testMapNewlineEntries() = doTest()
    fun testListNewlineKeywordEntry() = doTest()
    fun testModuleAttributeKeywordList() = doTest()
    fun testPipelineStartsOnNewline() = doTest()
    fun testFnClauseAfterNewline() = doTest()
    fun testStringKeyInKeywordList() = doTest()
    fun testNoParensCallWithDoBlock() = doTest()
    fun testDefaultArgInFunctionDef() = doTest()
    fun testMultiArgAnonymousFunction() = doTest()
    fun testNewlineAfterArrowInFn() = doTest()
    fun testTupleWithKeywordPair() = doTest()
    fun testUseWithAtomArg() = doTest()
    fun testMapEntryAfterComment() = doTest()
    fun testKeywordListValueOnNextLine() = doTest()
    fun testCaptureMatchMacro() = doTest()
    fun testCapturePlaceholderDotAccess() = doTest()
    fun testPipeCallMultilineKeywordArgs() = doTest()
    fun testFnClauseMapPatternWithListCons() = doTest()
    fun testMultilineMapKeywordPairs() = doTest()
    fun testUppercaseAtomInTuple() = doTest()
    fun testCaptureRemoteCallWithPlaceholder() = doTest()
    fun testTupleKeywordPairAfterNewline() = doTest()
    fun testTupleKeywordListAcrossLines() = doTest()
    fun testMultiClauseFnInCall() = doTest()
    fun testNoParensArgAfterCommaNewline() = doTest()
    fun testMultiArgFnPatternClause() = doTest()
    fun testAssocPairValueOnNextLine() = doTest()
    fun testPipelineAfterCaseExpr() = doTest()
    fun testSpecFunctionTypeArrow() = doTest()
    fun testAssertWithMessageComma() = doTest()
    fun testStructWithModuleVar() = doTest()
    fun testFnClauseInlineArgsAfterBody() = doTest()
    fun testCaptureTupleLiteral() = doTest()
    fun testCapturePlaceholderIdentity() = doTest()
    fun testDotAccessKeywordCallTarget() = doTest()
    fun testKeywordArgValueOnNextLine() = doTest()
    fun testMultiClauseFnGuard() = doTest()
    fun testOperatorAtomMatchRegex() = doTest()
    fun testSigilRegexEscapedDelimiter() = doTest()
    fun testStringInterpolationWithNestedQuotes() = doTest()
    fun testWhenGuardOnNextLine() = doTest()
    fun testNestedInterpolationInString() = doTest()
    fun testInterpolatedStringInspectMap() = doTest()
    fun testInterpolatedStringNestedInterpolation() = doTest()
    fun testInterpolatedSigil() = doTest()
    fun testCharlistInterpolation() = doTest()
    fun testCharlistHeredocInterpolation() = doTest()
    fun testInterpolationHeredocNested() = doTest()
    fun testInterpolationUnclosedInString() = doPartialTest()
    fun testCaptureSpecialFormNoParens() = doTest()
    fun testFnClauseGuardAfterArgsNewline() = doTest()
    fun testOperatorAtomDot() = doTest()
    fun testTypeUnionMultiline() = doTest()
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
    fun testStringEscapeInCall() = doTest()
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
    fun testCallNoParen() = doTest()
    fun testAmbiguousCall() = doTest()

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

    // =============================================================================
    // 11. Do-Blocks (Phase 7)
    // =============================================================================

    fun testDoBlockSimple() = doTest()
    fun testDoBlockMultiExpr() = doTest()
    fun testIfElse() = doTest()
    fun testTryCatch() = doTest()
    fun testTryAfter() = doTest()
    fun testTryRescue() = doTest()

    // Phase 10: Calls with Do-Blocks
    fun testIfSimple() = doTest()
    fun testIfOneLiner() = doTest()
    fun testUnless() = doTest()

    // Phase 11: Control Flow - case
    fun testCase() = doTest()
    fun testCaseMulti() = doTest()
    fun testCaseGuard() = doTest()

    // Phase 12: Control Flow - cond
    fun testCond() = doTest()

    // Phase 13: Control Flow - with
    fun testWith() = doTest()
    fun testWithElse() = doTest()

    // Phase 14: Control Flow - try
    fun testTry() = doTest()

    // Phase 15: Control Flow - receive
    fun testReceive() = doTest()
    fun testReceiveAfter() = doTest()

    // Phase 16: Module Definitions
    fun testDefmodule() = doTest()
    fun testDefmoduleNested() = doTest()

    // Phase 17: Function Definitions
    fun testDef() = doTest()
    fun testDefMultiClause() = doTest()
    fun testDefp() = doTest()

    // Phase 18: Macro Definitions
    fun testDefmacro() = doTest()
    fun testDefmacrop() = doTest()

    // Phase 19: Guard Definitions
    fun testDefguard() = doTest()

    // Phase 20: Struct and Exception Definitions
    fun testDefstruct() = doTest()
    fun testDefexception() = doTest()

    // Phase 21: Protocol Definitions
    fun testDefprotocol() = doTest()
    fun testDefimpl() = doTest()

    // Phase 22: Module Attributes
    fun testModuleAttr() = doTest()
    fun testModuleDoc() = doTest()
    fun testDoc() = doTest()
    fun testSpec() = doTest()
    fun testType() = doTest()
    fun testOpaque() = doTest()
    fun testCallback() = doTest()
    fun testBehaviour() = doTest()

    // Phase 23: Import/Require/Use
    fun testImport() = doTest()
    fun testImportOnly() = doTest()
    fun testImportExcept() = doTest()
    fun testRequire() = doTest()
    fun testUse() = doTest()
    fun testUseOpts() = doTest()

    // Phase 24: Alias Directive
    fun testAlias() = doTest()
    fun testAliasAs() = doTest()
    fun testAliasMulti() = doTest()

    // Phase 25: Comprehensions
    fun testForSimple() = doTest()
    fun testForFilter() = doTest()
    fun testForMultiGen() = doTest()
    fun testForInto() = doTest()
    fun testForUniq() = doTest()
    fun testForBitstring() = doTest()

    // Phase 26: Quote and Unquote
    fun testQuote() = doTest()
    fun testQuoteUnquote() = doTest()
    fun testQuoteSplice() = doTest()
    fun testQuoteBind() = doTest()

    // Phase 27: Raise, Throw, and Send
    fun testRaise() = doTest()
    fun testRaiseException() = doTest()
    fun testThrow() = doTest()
    fun testSend() = doTest()
    fun testSuper() = doTest()


    // Phase 30: Partial Parsing and Error Recovery
    fun testPartialListUnclosed() = doPartialTest()
    fun testPartialTupleUnclosed() = doPartialTest()
    fun testPartialMapUnclosed() = doPartialTest()
    fun testPartialBitstringUnclosed() = doPartialTest()
    fun testPartialParenUnclosed() = doPartialTest()
    fun testPartialStringUnclosed() = doPartialTest()
    fun testPartialHeredocUnclosed() = doPartialTest()
    fun testPartialSigilUnclosed() = doPartialTest()
    fun testPartialNestedUnclosed() = doPartialTest()
    fun testPartialDeeplyNested() = doPartialTest()

    fun testPartialDoNoEnd() = doPartialTest()
    fun testPartialDoEmpty() = doPartialTest()
    fun testPartialDoNestedNoEnd() = doPartialTest()
    fun testPartialDoOnlyEnd() = doPartialTest()
    fun testPartialFnNoEnd() = doPartialTest()
    fun testPartialFnNoArrow() = doPartialTest()
    fun testPartialCaseNoEnd() = doPartialTest()
    fun testPartialCaseNoArrow() = doPartialTest()
    fun testPartialWithNoEnd() = doPartialTest()
    fun testPartialTryNoEnd() = doPartialTest()
    fun testPartialReceiveNoEnd() = doPartialTest()
    fun testPartialCondNoEnd() = doPartialTest()

    fun testPartialDefmoduleNoEnd() = doPartialTest()
    fun testPartialDefmoduleNoBody() = doPartialTest()
    fun testPartialDefNoBody() = doPartialTest()
    fun testPartialDefNoEnd() = doPartialTest()
    fun testPartialDefIncompleteArgs() = doPartialTest()
    fun testPartialDefIncompleteGuard() = doPartialTest()
    fun testPartialDefmacroNoEnd() = doPartialTest()
    fun testPartialDefstructIncomplete() = doPartialTest()
    fun testPartialDefprotocolNoEnd() = doPartialTest()
    fun testPartialDefimplNoEnd() = doPartialTest()

    fun testPartialBinaryOpNoRhs() = doPartialTest()
    fun testPartialBinaryOpNoLhs() = doPartialTest()
    fun testPartialPipelineIncomplete() = doPartialTest()
    fun testPartialMatchNoRhs() = doPartialTest()
    fun testPartialComparisonNoRhs() = doPartialTest()
    fun testPartialRangeNoEnd() = doPartialTest()
    fun testPartialTypeSpecNoRhs() = doPartialTest()
    fun testPartialCaptureIncomplete() = doPartialTest()
    fun testPartialCaptureNoSlash() = doPartialTest()
    fun testPartialArrowNoRhs() = doPartialTest()
    fun testPartialWhenNoGuard() = doPartialTest()

    fun testPartialCallUnclosedParen() = doPartialTest()
    fun testPartialCallTrailingComma() = doPartialTest()
    fun testPartialCallNoArgs() = doPartialTest()
    fun testPartialCallKeywordNoValue() = doPartialTest()
    fun testPartialCallKeywordIncomplete() = doPartialTest()
    fun testPartialDotCallIncomplete() = doPartialTest()
    fun testPartialDotCallChained() = doPartialTest()
    fun testPartialAnonymousCall() = doPartialTest()
    fun testPartialBracketAccess() = doPartialTest()
    fun testPartialNestedCallIncomplete() = doPartialTest()

    fun testPartialListTrailingComma() = doPartialTest()
    fun testPartialListConsIncomplete() = doPartialTest()
    fun testPartialMapKeyNoValue() = doPartialTest()
    fun testPartialMapArrowNoValue() = doPartialTest()
    fun testPartialMapUpdateIncomplete() = doPartialTest()
    fun testPartialStructNoFields() = doPartialTest()
    fun testPartialStructIncomplete() = doPartialTest()
    fun testPartialKeywordNoValue() = doPartialTest()
    fun testPartialTupleTrailingComma() = doPartialTest()
    fun testPartialBitstringTyped() = doPartialTest()

    fun testPartialAliasIncomplete() = doPartialTest()
    fun testPartialAliasAsIncomplete() = doPartialTest()
    fun testPartialAliasMultiIncomplete() = doPartialTest()
    fun testPartialImportOnlyIncomplete() = doPartialTest()
    fun testPartialQuoteNoBody() = doPartialTest()
    fun testPartialUnquoteIncomplete() = doPartialTest()
    fun testPartialForNoBody() = doPartialTest()
    fun testPartialForGeneratorIncompl() = doPartialTest()
    fun testPartialWithClauseIncomplete() = doPartialTest()

    fun testPartialModuleWithIncomplete() = doPartialTest()
    fun testPartialNestedBlocksIncomplete() = doPartialTest()
    fun testPartialPipelineInBlock() = doPartialTest()
    fun testPartialCallInList() = doPartialTest()
    fun testPartialMapInCall() = doPartialTest()
    fun testPartialMultipleErrors() = doPartialTest()
    fun testPartialValidThenInvalid() = doPartialTest()
    fun testPartialInvalidThenValid() = doPartialTest()
    fun testPartialAtModuleLevel() = doPartialTest()
    fun testPartialInterpolationUnclosed() = doPartialTest()

    fun testRecoveryAfterBadExpr() = doPartialTest()
    fun testRecoverySkipBadStatement() = doPartialTest()
    fun testRecoveryInModuleBody() = doPartialTest()
    fun testRecoveryNestedStructures() = doPartialTest()
    fun testRecoveryContinueAfterEnd() = doPartialTest()
    fun testRecoveryMismatchedDelimiters() = doPartialTest()
}
