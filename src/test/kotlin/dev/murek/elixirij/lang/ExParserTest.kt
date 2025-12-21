package dev.murek.elixirij.lang

import com.intellij.testFramework.ParsingTestCase

class ExParserTest : ParsingTestCase("parser", "ex", ExParserDefinition()) {
    
    override fun getTestDataPath(): String = "src/test/testData"
    
    fun testSimpleInteger() = doTest(true)
    
    fun testSimpleString() = doTest(true)
    
    fun testSimpleAtom() = doTest(true)
    
    fun testSimpleIdentifier() = doTest(true)
    
    fun testSimpleAlias() = doTest(true)
    
    fun testMultipleExpressions() = doTest(true)
}
