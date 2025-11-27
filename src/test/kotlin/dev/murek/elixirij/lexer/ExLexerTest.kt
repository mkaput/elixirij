package dev.murek.elixirij.lexer

import com.intellij.lexer.Lexer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.LexerTestCase
import com.intellij.testFramework.UsefulTestCase
import java.io.File
import java.io.IOException

/**
 * Tests for the Elixir lexer using fixture files.
 * Each test loads a `.ex` source file and compares lexer output with the corresponding `.txt` file.
 */
class ExLexerTest : LexerTestCase() {

    override fun createLexer(): Lexer = ExLexer()
    override fun getDirPath(): String = "lexer"

    private fun getTestDataPath(): String = "src/test/testData"

    private fun doTest() {
        val testName = getTestNameForFixture()
        val testDataPath = getTestDataPath()
        val sourceFile = File("$testDataPath/$dirPath/$testName.ex")
        val goldFile = File("$testDataPath/$dirPath/$testName.txt")

        var text = ""
        try {
            val fileText = FileUtil.loadFile(sourceFile, Charsets.UTF_8)
            text = StringUtil.convertLineSeparators(if (shouldTrim()) fileText.trim() else fileText)
        } catch (e: IOException) {
            fail("Can't load file $sourceFile: ${e.message}")
        }

        val result = printTokens(text, 0, createLexer())
        UsefulTestCase.assertSameLinesWithFile(goldFile.canonicalPath, result)
    }

    /**
     * Converts the test method name to the fixture file name.
     * For a test method like `test integer literals`, returns "integer_literals".
     */
    private fun getTestNameForFixture(): String {
        // Get the raw test name which will be something like "test integer literals" 
        // or "testIntegerLiterals" depending on how it's defined
        val rawName = name ?: throw IllegalStateException("Test name is null")
        
        // Remove "test " or "test" prefix
        val withoutPrefix = when {
            rawName.startsWith("test ") -> rawName.removePrefix("test ")
            rawName.startsWith("test") -> rawName.removePrefix("test")
            else -> rawName
        }
        
        // Convert spaces to underscores and handle camelCase
        return withoutPrefix
            .replace(' ', '_')
            .replace(Regex("([a-z])([A-Z])"), "$1_$2")
            .lowercase()
    }

    fun `test integer literals`() = doTest()
    fun `test float literals`() = doTest()
    fun `test keywords`() = doTest()
    fun `test identifiers`() = doTest()
    fun `test aliases`() = doTest()
    fun `test atoms`() = doTest()
    fun `test quoted atoms`() = doTest()
    fun `test strings`() = doTest()
    fun `test charlists`() = doTest()
    fun `test comments`() = doTest()
    fun `test operators`() = doTest()
    fun `test delimiters`() = doTest()
    fun `test char literals`() = doTest()
    fun `test range operators`() = doTest()
    fun `test simple elixir function`() = doTest()
}
