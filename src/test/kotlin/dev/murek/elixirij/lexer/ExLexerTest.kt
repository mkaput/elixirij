package dev.murek.elixirij.lexer

import com.intellij.lexer.Lexer
import com.intellij.testFramework.LexerTestCase
import java.io.File

/**
 * Tests for the Elixir lexer using fixture files.
 */
class ExLexerTest : LexerTestCase() {

    override fun createLexer(): Lexer = ExLexer()

    override fun getDirPath(): String = "src/test/testData/lexer"

    override fun getPathToTestDataFile(extension: String): String {
        return File(dirPath, getTestName(true) + extension).absolutePath
    }

    fun testIntegerLiterals() { doTest() }
    fun testFloatLiterals() { doTest() }
    fun testKeywords() { doTest() }
    fun testIdentifiers() { doTest() }
    fun testAliases() { doTest() }
    fun testAtoms() { doTest() }
    fun testQuotedAtoms() { doTest() }
    fun testStrings() { doTest() }
    fun testCharlists() { doTest() }
    fun testComments() { doTest() }
    fun testOperators() { doTest() }
    fun testDelimiters() { doTest() }
    fun testCharLiterals() { doTest() }
    fun testRangeOperators() { doTest() }
    fun testSimpleElixirFunction() { doTest() }

    private fun doTest() {
        doFileTest("ex")
    }
}
