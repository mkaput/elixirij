package dev.murek.elixirij.ide

import com.intellij.codeInsight.highlighting.BraceMatchingUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.murek.elixirij.ExLanguage
import org.intellij.lang.annotations.Language

class ExBraceMatcherTest : BasePlatformTestCase() {

    fun `test matches do end braces`() = doBraceMatchTest(
        """
        defmodule Foo <brace>do
          :ok
        <brace>end
        """.trimIndent()
    )

    fun `test matches fn end braces`() = doBraceMatchTest(
        """
        <brace>fn x ->
          x + 1
        <brace>end
        """.trimIndent()
    )

    fun `test matches parenthesis braces`() = doBraceMatchTest(
        "sum = <brace>(a + b<brace>)"
    )

    fun `test matches bracket braces`() = doBraceMatchTest(
        "list = <brace>[1, 2, 3<brace>]"
    )

    fun `test matches brace braces`() = doBraceMatchTest(
        "tuple = <brace>{:ok, 1<brace>}"
    )

    fun `test matches map braces`() = doBraceMatchTest(
        "map = <brace>%{foo: 1<brace>}"
    )

    fun `test matches binary braces`() = doBraceMatchTest(
        "bin = <brace><<1, 2<brace>>>"
    )

    fun `test matches interpolation braces`() = doBraceMatchTest(
        "\"value: <brace>#{value<brace>}\""
    )

    fun `test code construct start returns opening brace offset for simple braces`() {
        myFixture.configureByText("test.ex", "()")
        val braceMatcher = BraceMatchingUtil.getBraceMatcher(myFixture.file.fileType, ExLanguage)

        assertEquals(0, braceMatcher.getCodeConstructStart(myFixture.file, 0))
    }

    fun `test code construct start navigates to call for do block`() {
        val code = "defmodule Foo do\nend"
        myFixture.configureByText("test.ex", code)

        val braceMatcher = BraceMatchingUtil.getBraceMatcher(myFixture.file.fileType, ExLanguage)
        val doOffset = code.indexOf(" do") + 1
        assertEquals(0, braceMatcher.getCodeConstructStart(myFixture.file, doOffset))
    }

    fun `test code construct start navigates to call for paren call with do block`() {
        val code = "if(true) do\nend"
        myFixture.configureByText("test.ex", code)

        val braceMatcher = BraceMatchingUtil.getBraceMatcher(myFixture.file.fileType, ExLanguage)
        val doOffset = code.indexOf(" do") + 1
        assertEquals(0, braceMatcher.getCodeConstructStart(myFixture.file, doOffset))
    }

    private fun doBraceMatchTest(@Language("Elixir") textWithMarkers: String) {
        val firstMarkerOffset = textWithMarkers.indexOf(BRACE_MARKER)
        assertTrue("Expected first $BRACE_MARKER marker", firstMarkerOffset >= 0)

        var text = textWithMarkers.replaceFirst(BRACE_MARKER, "")
        val secondMarkerOffset = text.indexOf(BRACE_MARKER)
        assertTrue("Expected second $BRACE_MARKER marker", secondMarkerOffset >= 0)
        text = text.replaceFirst(BRACE_MARKER, "")

        assertBraceMatch(text, firstMarkerOffset, secondMarkerOffset, "first")
        assertBraceMatch(text, secondMarkerOffset, firstMarkerOffset, "second")
    }

    private fun assertBraceMatch(
        @Language("Elixir") text: String,
        caretOffset: Int,
        expectedOffset: Int,
        label: String
    ) {
        val textWithCaret = StringBuilder(text).insert(caretOffset, CARET_MARKER).toString()
        myFixture.configureByText("test.ex", textWithCaret)

        val forward = expectedOffset > caretOffset
        val actualOffset = BraceMatchingUtil.getMatchedBraceOffset(myFixture.editor, forward, myFixture.file)
        assertEquals("Expected matching brace for $label marker", expectedOffset, actualOffset)
    }

    private companion object {
        private const val BRACE_MARKER = "<brace>"
        private const val CARET_MARKER = "<caret>"
    }
}
