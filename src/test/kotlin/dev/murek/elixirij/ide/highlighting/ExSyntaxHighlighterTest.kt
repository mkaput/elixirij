package dev.murek.elixirij.ide.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.psi.tree.IElementType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.murek.elixirij.lang.EX_INTERPOLATION_END
import dev.murek.elixirij.lang.EX_INTERPOLATION_START
import dev.murek.elixirij.lang.EX_STRING_PART

class ExSyntaxHighlighterTest : BasePlatformTestCase() {

    fun `test interpolation tokens are highlighted in strings`() {
        val code = "\"a #{b} c\""
        assertTokenHighlighted(code, EX_STRING_PART, ExTextAttributes.STRING.attribute)
        assertTokenHighlighted(code, EX_INTERPOLATION_START, ExTextAttributes.BRACES.attribute)
        assertTokenHighlighted(code, EX_INTERPOLATION_END, ExTextAttributes.BRACES.attribute)
    }

    fun `test interpolation tokens are highlighted in sigils`() {
        val code = "~s(foo #{bar})"
        assertTokenHighlighted(code, EX_INTERPOLATION_START, ExTextAttributes.BRACES.attribute)
        assertTokenHighlighted(code, EX_INTERPOLATION_END, ExTextAttributes.BRACES.attribute)
    }

    private fun assertTokenHighlighted(
        code: String,
        tokenType: IElementType,
        expected: TextAttributesKey
    ) {
        val highlighter = ExSyntaxHighlighter()
        val lexer = highlighter.highlightingLexer
        lexer.start(code)

        var found = false
        while (lexer.tokenType != null) {
            val current = lexer.tokenType
            if (current == tokenType) {
                found = true
                val highlights = highlighter.getTokenHighlights(tokenType)
                assertTrue(
                    "Expected $tokenType to use ${expected.externalName}",
                    highlights.any { it == expected }
                )
            }
            lexer.advance()
        }

        assertTrue("Expected token $tokenType in: $code", found)
    }
}
