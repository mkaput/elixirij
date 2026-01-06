package dev.murek.elixirij.lang.lexer

import com.intellij.lexer.Lexer
import com.intellij.testFramework.LexerTestCase
import java.io.File

class ExLexerTest : LexerTestCase() {

    override fun createLexer(): Lexer = ExLexer()

    override fun getDirPath(): String = "src/test/testData/lexer"

    override fun getPathToTestDataFile(extension: String): String =
        File(dirPath, getTestName(true) + extension).absolutePath

    fun testIntegerLiterals() = doTest()
    fun testFloatLiterals() = doTest()
    fun testKeywords() = doTest()
    fun testIdentifiers() = doTest()
    fun testAliases() = doTest()
    fun testAtoms() = doTest()
    fun testQuotedAtoms() = doTest()
    fun testStrings() = doTest()
    fun testCharlists() = doTest()
    fun testComments() = doTest()
    fun testOperators() = doTest()
    fun testDelimiters() = doTest()
    fun testCharLiterals() = doTest()
    fun testRangeOperators() = doTest()
    fun testSimpleElixirFunction() = doTest()
    fun testHexBinOctal() = doTest()
    fun testUnderscoresInNumbers() = doTest()
    fun testScientificNotation() = doTest()
    fun testOperatorAtoms() = doTest()
    fun testIdentifiersWithSuffix() = doTest()
    fun testModuleMacros() = doTest()
    fun testDotOperator() = doTest()
    fun testInterpolation() = doTest()
    fun testSigils() = doTest()
    fun testHeredocs() = doTest()
    fun testEscapes_strings() = doTest()
    fun testEscapes_charlists_and_sigils() = doTest()

    private fun doTest() = doFileTest("ex")
}
