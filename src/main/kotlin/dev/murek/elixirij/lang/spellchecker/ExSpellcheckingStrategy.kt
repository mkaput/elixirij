package dev.murek.elixirij.lang.spellchecker

import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.tokenizer.EscapeSequenceTokenizer
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.TokenConsumer
import com.intellij.spellchecker.tokenizer.Tokenizer
import dev.murek.elixirij.lang.EX_STRINGS
import org.jetbrains.annotations.NotNull

/**
 * Spellchecking strategy for Elixir language.
 *
 * Enables spell checking in:
 * - Comments
 * - String literals (double-quoted strings and heredocs)
 * - Charlist literals (single-quoted strings and charlists heredocs)
 * - Text sigils (~s, ~S, etc.)
 *
 * Disables spell checking in:
 * - Code identifiers (variable/function names)
 * - Atoms
 * - Module aliases
 * - Keywords
 * - Regex sigils (~r, ~R)
 */
class ExSpellcheckingStrategy : SpellcheckingStrategy(), DumbAware {

    private val stringTokenizer = object : EscapeSequenceTokenizer<PsiElement>() {
        override fun tokenize(@NotNull element: PsiElement, @NotNull consumer: TokenConsumer) {
            val text = element.text

            // Remove quotes (either single, double, or heredoc triple quotes)
            val content = when {
                text.startsWith("\"\"\"") && text.endsWith("\"\"\"") ->
                    text.substring(3, text.length - 3)
                text.startsWith("'''") && text.endsWith("'''") ->
                    text.substring(3, text.length - 3)
                text.startsWith('"') && text.endsWith('"') ->
                    text.substring(1, text.length - 1)
                text.startsWith('\'') && text.endsWith('\'') ->
                    text.substring(1, text.length - 1)
                else -> text
            }

            // Check if the string contains escape sequences
            if (!content.contains('\\')) {
                // No escape sequences, use plain text splitter
                consumer.consumeToken(element, PlainTextSplitter.getInstance())
            } else {
                // Has escape sequences, handle them properly
                // For now, use plain text splitter on the unescaped content
                // TODO: Implement proper escape sequence handling if needed
                consumer.consumeToken(element, PlainTextSplitter.getInstance())
            }
        }
    }

    private val sigilTokenizer = object : Tokenizer<PsiElement>() {
        override fun tokenize(@NotNull element: PsiElement, @NotNull consumer: TokenConsumer) {
            val text = element.text

            // Check if this is a regex sigil (~r or ~R) - don't spell check those
            if (text.matches(Regex("~[rR].*"))) {
                return
            }

            // For other sigils (text sigils like ~s, ~S), spell check the content
            // Extract content between delimiters and modifiers
            // Sigils have format: ~s{content}modifiers or ~s"content"modifiers, etc.
            val sigilPattern = Regex("~[a-zA-Z](.)(.*?)\\1[a-zA-Z]*", RegexOption.DOT_MATCHES_ALL)
            val match = sigilPattern.matchEntire(text)

            if (match != null) {
                // Use plain text splitter for sigil content
                consumer.consumeToken(element, PlainTextSplitter.getInstance())
            }
        }
    }

    override fun getTokenizer(element: PsiElement): Tokenizer<*> {
        val elementType = element.node?.elementType

        return when {
            // Enable spell checking for comments
            element is PsiComment -> super.getTokenizer(element)

            // Enable spell checking for strings and charlists
            elementType != null && EX_STRINGS.contains(elementType) -> stringTokenizer

            // Enable spell checking for text sigils (but not regex sigils)
            elementType?.toString() == "SIGIL" -> sigilTokenizer

            // Disable spell checking for everything else (identifiers, atoms, keywords, etc.)
            else -> EMPTY_TOKENIZER
        }
    }
}
