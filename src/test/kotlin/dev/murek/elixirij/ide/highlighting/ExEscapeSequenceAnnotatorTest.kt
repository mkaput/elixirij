package dev.murek.elixirij.ide.highlighting

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ExEscapeSequenceAnnotatorTest : BasePlatformTestCase() {

    fun `test invalid escape tokens are reported`() {
        doTest(
            """
            defmodule Sample do
              def bad do
                "<error descr=\"Invalid escape sequence\">\\xZ</error>"
                "<error descr=\"Invalid Unicode escape sequence\">\\u12</error>"
                "<error descr=\"Invalid Unicode escape sequence\">\\u{}</error>"
              end
            end
            """.trimIndent()
        )
    }

    fun `test invalid unicode code point is reported`() {
        doTest(
            """
            defmodule Sample do
              def bad do
                "<error descr=\"Invalid Unicode code point in escape sequence\">\\u{110000}</error>"
              end
            end
            """.trimIndent()
        )
    }

    private fun doTest(code: String) {
        myFixture.configureByText("test.ex", code)
        myFixture.checkHighlighting(false, false, false, true)
    }
}
