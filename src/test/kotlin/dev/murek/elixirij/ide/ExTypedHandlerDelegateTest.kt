package dev.murek.elixirij.ide

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Tests for [ExTypedHandlerDelegate] to verify auto-pairing behavior.
 *
 * Tests cover:
 * - Heredoc auto-completion (""" and ''')
 * - Sigil delimiter auto-pairing (~r[, ~s(, etc.)
 */
class ExTypedHandlerDelegateTest : BasePlatformTestCase() {
    // Heredoc tests
    fun `test string heredoc auto-completion`() {
        myFixture.configureByText("test.ex", "\"\"<caret>")
        myFixture.type('"')
        myFixture.checkResult("\"\"\"<caret>\"\"\"")
    }

    fun `test charlist heredoc auto-completion`() {
        myFixture.configureByText("test.ex", "''<caret>")
        myFixture.type('\'')
        myFixture.checkResult("'''<caret>'''")
    }

    fun `test heredoc caret position after completion`() {
        myFixture.configureByText("test.ex", "\"\"<caret>")
        myFixture.type('"')
        myFixture.checkResult("\"\"\"<caret>\"\"\"")
    }

    fun `test no duplicate heredoc when closing exists`() {
        myFixture.configureByText("test.ex", "\"\"<caret>\n\"\"\"")
        myFixture.type('"')
        myFixture.checkResult("\"\"\"<caret>\n\"\"\"")
    }

    // Sigil delimiter tests
    fun `test sigil bracket auto-pairing`() {
        myFixture.configureByText("test.ex", "~r<caret>")
        myFixture.type('[')
        myFixture.checkResult("~r[<caret>]")
    }

    fun `test sigil paren auto-pairing`() {
        myFixture.configureByText("test.ex", "~s<caret>")
        myFixture.type('(')
        myFixture.checkResult("~s(<caret>)")
    }

    fun `test sigil brace auto-pairing`() {
        myFixture.configureByText("test.ex", "~w<caret>")
        myFixture.type('{')
        myFixture.checkResult("~w{<caret>}")
    }

    fun `test sigil angle bracket auto-pairing`() {
        myFixture.configureByText("test.ex", "~c<caret>")
        myFixture.type('<')
        myFixture.checkResult("~c<<caret>>")
    }

    fun `test sigil slash auto-pairing`() {
        myFixture.configureByText("test.ex", "~r<caret>")
        myFixture.type('/')
        myFixture.checkResult("~r/<caret>/")
    }

    fun `test sigil pipe auto-pairing`() {
        myFixture.configureByText("test.ex", "~r<caret>")
        myFixture.type('|')
        myFixture.checkResult("~r|<caret>|")
    }

    fun `test sigil caret position after auto-pairing`() {
        myFixture.configureByText("test.ex", "~r<caret>")
        myFixture.type('[')
        myFixture.checkResult("~r[<caret>]")
    }

    fun `test interpolation brace auto-pairing in string`() {
        myFixture.configureByText("test.ex", "\"<caret>\"")
        myFixture.type('#')
        myFixture.type('{')
        myFixture.checkResult("\"#{<caret>}\"")
    }

    fun `test interpolation brace auto-pairing in sigil`() {
        myFixture.configureByText("test.ex", "~s(<caret>)")
        myFixture.type('#')
        myFixture.type('{')
        myFixture.checkResult("~s(#{<caret>})")
    }

    fun `test uppercase sigil auto-pairing`() {
        myFixture.configureByText("test.ex", "~R<caret>")
        myFixture.type('[')
        myFixture.checkResult("~R[<caret>]")
    }

    fun `test no sigil auto-pair when not after sigil prefix`() {
        myFixture.configureByText("test.ex", "x<caret>")
        myFixture.type('[')
        // Should use default bracket handling, not sigil handling
        myFixture.checkResult("x[<caret>]")
    }

    fun `test consecutive quote typing`() {
        myFixture.configureByText("test.ex", "<caret>")
        myFixture.type('"')
        myFixture.checkResult("\"<caret>\"")

        myFixture.type('"')
        myFixture.checkResult("\"\"<caret>")

        myFixture.type('"')
        myFixture.checkResult("\"\"\"<caret>\"\"\"")

        myFixture.type('"')
        myFixture.checkResult("\"\"\"\"<caret>\"\"")
    }

    fun `test consecutive single quote typing`() {
        myFixture.configureByText("test.ex", "<caret>")
        myFixture.type('\'')
        myFixture.checkResult("'<caret>'")

        myFixture.type('\'')
        myFixture.checkResult("''<caret>")

        myFixture.type('\'')
        myFixture.checkResult("'''<caret>'''")

        myFixture.type('\'')
        myFixture.checkResult("''''<caret>''")
    }

    fun `test consecutive sigil delimiter typing`() {
        myFixture.configureByText("test.ex", "~r<caret>")
        myFixture.type('/')
        myFixture.checkResult("~r/<caret>/")

        myFixture.type('/')
        myFixture.checkResult("~r//<caret>/")

        myFixture.type('/')
        myFixture.checkResult("~r///<caret>/")
    }

    fun `test quote auto-pairing before expression below`() {
        myFixture.configureByText(
            "test.ex",
            """
                def foo() do
                    tmp = <caret>

                    result = "abc" <> "def"
                    result
                end
            """.trimIndent()
        )
        myFixture.type('"')
        myFixture.checkResult(
            """
                def foo() do
                    tmp = "<caret>"

                    result = "abc" <> "def"
                    result
                end
            """.trimIndent()
        )
    }
}
