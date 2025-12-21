package dev.murek.elixirij.lang

import com.intellij.testFramework.ParsingTestCase

class ExParserTest : ParsingTestCase("parser", "ex", ExParserDefinition()) {

    override fun getTestDataPath(): String = "src/test/testData"
}
