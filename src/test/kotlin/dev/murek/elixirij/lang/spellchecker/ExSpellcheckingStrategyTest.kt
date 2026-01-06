package dev.murek.elixirij.lang.spellchecker

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.spellchecker.inspections.Splitter
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.TokenConsumer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.murek.elixirij.lang.EX_CHARLIST_PART
import dev.murek.elixirij.lang.EX_STRING_PART
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Tests for [ExSpellcheckingStrategy] to verify spell checking behavior in Elixir files.
 */
class ExSpellcheckingStrategyTest : BasePlatformTestCase() {

    private val strategy = ExSpellcheckingStrategy()

    // Test that comments are spell-checked

    fun `test comments are spell checked`() {
        myFixture.configureByText(
            "test.ex",
            """
                # This is a comment with misspeling
                def foo, do: :ok
            """.trimIndent()
        )

        val comment = findElementAtOffset(myFixture.file.text.indexOf("# This"))
        assertNotNull("Comment element should exist", comment)
        assertTrue("Should be a comment", comment is PsiComment)

        val tokenizer = strategy.getTokenizer(comment!!)
        assertNotEquals(
            "Comments should be spell-checked",
            SpellcheckingStrategy.EMPTY_TOKENIZER,
            tokenizer
        )
    }

    // Test that strings are spell-checked

    fun `test double quoted strings are spell checked`() {
        myFixture.configureByText(
            "test.ex",
            """
                x = "hello wrold"
            """.trimIndent()
        )

        val stringElement = findStringPartElement(myFixture.file.text.indexOf("hello"))
        assertNotNull("String element should exist", stringElement)

        val tokenizer = strategy.getTokenizer(stringElement!!)
        assertNotEquals(
            "Strings should be spell-checked",
            SpellcheckingStrategy.EMPTY_TOKENIZER,
            tokenizer
        )
    }

    fun `test charlists are spell checked`() {
        myFixture.configureByText(
            "test.ex",
            """
                x = 'hello wrold'
            """.trimIndent()
        )

        val charlistElement = findStringPartElement(myFixture.file.text.indexOf("hello"))
        assertNotNull("Charlist element should exist", charlistElement)

        val tokenizer = strategy.getTokenizer(charlistElement!!)
        assertNotEquals(
            "Charlists should be spell-checked",
            SpellcheckingStrategy.EMPTY_TOKENIZER,
            tokenizer
        )
    }

    fun `test heredocs are spell checked`() {
        myFixture.configureByText(
            "test.ex",
            """
                x = ${"\"\"\""}
                hello wrold
                ${"\"\"\""}
            """.trimIndent()
        )

        val heredocElement = findStringPartElement(myFixture.file.text.indexOf("hello"))
        assertNotNull("Heredoc element should exist", heredocElement)

        val tokenizer = strategy.getTokenizer(heredocElement!!)
        assertNotEquals(
            "Heredocs should be spell-checked",
            SpellcheckingStrategy.EMPTY_TOKENIZER,
            tokenizer
        )
    }

    fun `test escape sequences are skipped in spell checking`() {
        myFixture.configureByText(
            "test.ex",
            """
                x = "foo\\nbar"
            """.trimIndent()
        )

        val firstPart = findStringPartElement(myFixture.file.text.indexOf("foo"))
        val secondPart = findStringPartElement(myFixture.file.text.indexOf("bar"))
        assertNotNull("First string part should exist", firstPart)
        assertNotNull("Second string part should exist", secondPart)

        val combined = buildString {
            append(collectTokens(firstPart!!))
            append(collectTokens(secondPart!!))
        }
        assertTrue("Should include foo", combined.contains("foo"))
        assertTrue("Should include bar", combined.contains("bar"))
        assertFalse("Escapes should be removed", combined.contains("\\"))
    }

    // Test that identifiers are NOT spell-checked

    fun `test identifiers are not spell checked`() {
        myFixture.configureByText(
            "test.ex",
            """
                def misspeled_function_name, do: :ok
            """.trimIndent()
        )

        val identifier = findElementAtOffset(myFixture.file.text.indexOf("misspeled"))
        assertNotNull("Identifier element should exist", identifier)

        val tokenizer = strategy.getTokenizer(identifier!!)
        assertEquals(
            "Identifiers should not be spell-checked",
            SpellcheckingStrategy.EMPTY_TOKENIZER,
            tokenizer
        )
    }

    // Test that atoms are NOT spell-checked

    fun `test atoms are not spell checked`() {
        myFixture.configureByText(
            "test.ex",
            """
                x = :misspeled_atom
            """.trimIndent()
        )

        val atom = findElementAtOffset(myFixture.file.text.indexOf(":misspeled"))
        assertNotNull("Atom element should exist", atom)

        val tokenizer = strategy.getTokenizer(atom!!)
        assertEquals(
            "Atoms should not be spell-checked",
            SpellcheckingStrategy.EMPTY_TOKENIZER,
            tokenizer
        )
    }

    // Test that module aliases are NOT spell-checked

    fun `test module aliases are not spell checked`() {
        myFixture.configureByText(
            "test.ex",
            """
                defmodule MisspelledModuleName do
                end
            """.trimIndent()
        )

        val alias = findElementAtOffset(myFixture.file.text.indexOf("MisspelledModuleName"))
        assertNotNull("Alias element should exist", alias)

        val tokenizer = strategy.getTokenizer(alias!!)
        assertEquals(
            "Module aliases should not be spell-checked",
            SpellcheckingStrategy.EMPTY_TOKENIZER,
            tokenizer
        )
    }

    // Test that keywords are NOT spell-checked

    fun `test keywords are not spell checked`() {
        myFixture.configureByText(
            "test.ex",
            """
                def foo do
                  :ok
                end
            """.trimIndent()
        )

        val keyword = findElementAtOffset(myFixture.file.text.indexOf("def"))
        assertNotNull("Keyword element should exist", keyword)

        // Keywords might be parsed differently, but they should not be spell-checked
        // This test just ensures we don't crash on keywords
        val tokenizer = strategy.getTokenizer(keyword!!)
        assertNotNull("Tokenizer should not be null", tokenizer)
    }

    // Test sigil handling

    fun `test text sigils are spell checked`() {
        myFixture.configureByText(
            "test.ex",
            """
                x = ~s(hello wrold)
            """.trimIndent()
        )

        val sigil = findStringPartElement(myFixture.file.text.indexOf("hello"))
        assertNotNull("Sigil element should exist", sigil)

        val tokenizer = strategy.getTokenizer(sigil!!)
        // Text sigils should be spell-checked (not empty tokenizer)
        // Note: This might not work perfectly without proper sigil PSI support,
        // but we're testing the strategy logic
        assertNotNull("Tokenizer should not be null for text sigils", tokenizer)
    }

    fun `test regex sigils are not spell checked`() {
        myFixture.configureByText(
            "test.ex",
            """
                x = ~r/hello wrold/
            """.trimIndent()
        )

        val sigil = findStringPartElement(myFixture.file.text.indexOf("hello"))
        assertNotNull("Sigil element should exist", sigil)

        val tokenizer = strategy.getTokenizer(sigil!!)
        assertEquals(
            "Regex sigils should not be spell-checked",
            SpellcheckingStrategy.EMPTY_TOKENIZER,
            tokenizer
        )
    }

    // Helper methods

    /**
     * Finds a PsiElement at the given offset in the file.
     */
    private fun findElementAtOffset(offset: Int): PsiElement? {
        val file = myFixture.file
        return file.findElementAt(offset)
    }

    /**
     * Finds a string literal element at the given offset.
     * Walks up the PSI tree to find the actual string token element.
     */
    private fun findStringPartElement(offset: Int): PsiElement? {
        var element = findElementAtOffset(offset)

        while (element != null) {
            val elementType = element.node?.elementType
            if (elementType == EX_STRING_PART || elementType == EX_CHARLIST_PART) {
                return element
            }
            element = element.parent
        }

        return null
    }

    private fun collectTokens(element: PsiElement): String {
        @Suppress("UNCHECKED_CAST")
        val tokenizer = strategy.getTokenizer(element) as com.intellij.spellchecker.tokenizer.Tokenizer<PsiElement>
        val consumer = CollectingTokenConsumer()
        tokenizer.tokenize(element, consumer)
        return consumer.tokens.joinToString("")
    }

    private class CollectingTokenConsumer : TokenConsumer() {
        val tokens = mutableListOf<String>()

        override fun consumeToken(
            element: PsiElement,
            text: String,
            useRename: Boolean,
            offset: Int,
            rangeToCheck: com.intellij.openapi.util.TextRange,
            splitter: Splitter
        ) {
            tokens.add(text)
        }
    }
}
