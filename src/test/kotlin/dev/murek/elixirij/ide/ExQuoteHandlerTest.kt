package dev.murek.elixirij.ide

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ExQuoteHandlerTest : BasePlatformTestCase() {
    /**
     * Quote auto-pairing for empty contexts should insert matching closing quotes.
     */
    fun `test double quote typing`() {
        myFixture.configureByText("test.ex", "x = <caret>")
        myFixture.type('"')
        myFixture.checkResult("x = \"<caret>\"")
    }

    fun `test single quote typing`() {
        myFixture.configureByText("test.ex", "x = <caret>")
        myFixture.type('\'')
        myFixture.checkResult("x = '<caret>'")
    }

    fun `test quoted atom double quote typing`() {
        myFixture.configureByText("test.ex", "x = :<caret>")
        myFixture.type('"')
        myFixture.checkResult("x = :\"<caret>\"")
    }

    fun `test quoted atom single quote typing`() {
        myFixture.configureByText("test.ex", "x = :<caret>")
        myFixture.type('\'')
        myFixture.checkResult("x = :'<caret>'")
    }

    fun `test overtype closing double quote`() {
        myFixture.configureByText("test.ex", "x = \"hello<caret>\"")
        myFixture.type('"')
        // Should overtype the closing quote instead of inserting a new one
        myFixture.checkResult("x = \"hello\"<caret>")
    }

    fun `test overtype closing single quote`() {
        myFixture.configureByText("test.ex", "x = 'hello<caret>'")
        myFixture.type('\'')
        // Should overtype the closing quote instead of inserting a new one
        myFixture.checkResult("x = 'hello'<caret>")
    }

    fun `test overtype closing sigil double quote`() {
        myFixture.configureByText("test.ex", "x = ~s\"hello<caret>\"")
        myFixture.type('"')
        myFixture.checkResult("x = ~s\"hello\"<caret>")
    }

    fun `test overtype closing sigil single quote`() {
        myFixture.configureByText("test.ex", "x = ~s'hello<caret>'")
        myFixture.type('\'')
        myFixture.checkResult("x = ~s'hello'<caret>")
    }
}
