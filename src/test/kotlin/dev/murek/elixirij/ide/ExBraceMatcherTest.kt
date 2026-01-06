package dev.murek.elixirij.ide

import com.intellij.lang.BracePair
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.murek.elixirij.lang.*

class ExBraceMatcherTest : BasePlatformTestCase() {

    private val braceMatcher = ExBraceMatcher()

    fun `test gets all brace pairs`() {
        val pairs = braceMatcher.pairs

        assertEquals(8, pairs.size)

        // Structural pairs (do...end, fn...end)
        assertContainsPair(pairs, EX_DO, EX_END, structural = true)
        assertContainsPair(pairs, EX_FN, EX_END, structural = true)

        // Non-structural pairs
        assertContainsPair(pairs, EX_LPAREN, EX_RPAREN, structural = false)
        assertContainsPair(pairs, EX_LBRACKET, EX_RBRACKET, structural = false)
        assertContainsPair(pairs, EX_LBRACE, EX_RBRACE, structural = false)
        assertContainsPair(pairs, EX_PERCENT_LBRACE, EX_RBRACE, structural = false)
        assertContainsPair(pairs, EX_LT_LT, EX_GT_GT, structural = false)
        assertContainsPair(pairs, EX_INTERPOLATION_START, EX_INTERPOLATION_END, structural = false)
    }

    fun `test paired braces allowed before any type`() {
        val result = braceMatcher.isPairedBracesAllowedBeforeType(EX_LPAREN, EX_IDENTIFIER)
        assertTrue(result)
    }

    fun `test code construct start returns opening brace offset for simple braces`() {
        myFixture.configureByText("test.ex", "()")
        val offset = 0
        val result = braceMatcher.getCodeConstructStart(myFixture.file, offset)
        assertEquals(offset, result)
    }

    fun `test code construct start navigates to call for do block`() {
        val code = "defmodule Foo do\nend"
        myFixture.configureByText("test.ex", code)

        // "do" keyword starts at offset 14 (after "defmodule Foo ")
        val doOffset = code.indexOf(" do") + 1
        val result = braceMatcher.getCodeConstructStart(myFixture.file, doOffset)
        // Should navigate to "defmodule" at offset 0
        assertEquals(0, result)
    }

    fun `test code construct start navigates to call for paren call with do block`() {
        val code = "if(true) do\nend"
        myFixture.configureByText("test.ex", code)

        // "do" starts after "if(true) "
        val doOffset = code.indexOf(" do") + 1
        val result = braceMatcher.getCodeConstructStart(myFixture.file, doOffset)
        // Should navigate to "if" at offset 0
        assertEquals(0, result)
    }

    private fun assertContainsPair(
        pairs: Array<BracePair>,
        left: IElementType,
        right: IElementType,
        structural: Boolean
    ) {
        val pair = pairs.find { it.leftBraceType == left && it.rightBraceType == right }
        assertNotNull("Expected to find pair ($left, $right)", pair)
        assertEquals("Structural flag mismatch for ($left, $right)", structural, pair!!.isStructural)
    }
}
