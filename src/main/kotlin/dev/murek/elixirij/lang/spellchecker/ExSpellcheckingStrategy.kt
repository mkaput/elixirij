package dev.murek.elixirij.lang.spellchecker

import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiComment
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.spellchecker.inspections.PlainTextSplitter
import com.intellij.spellchecker.tokenizer.EscapeSequenceTokenizer
import com.intellij.spellchecker.tokenizer.SpellcheckingStrategy
import com.intellij.spellchecker.tokenizer.TokenConsumer
import com.intellij.spellchecker.tokenizer.Tokenizer
import dev.murek.elixirij.lang.EX_CHARLIST_PART
import dev.murek.elixirij.lang.EX_STRING_PART
import org.jetbrains.annotations.NotNull

/**
 * Spellchecking strategy for Elixir language.
 *
 * Enables spell checking in:
 * - Comments
 * - String literal parts (double-quoted strings and heredocs)
 * - Charlist literal parts (single-quoted strings and charlists heredocs)
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
            val content = extractStringContent(text) ?: return

            if (content.value.isEmpty()) return
            if (!content.value.contains('\\')) {
                consumer.consumeToken(element, PlainTextSplitter.getInstance())
                return
            }

            val (unescapedText, offsets) = parseEscapes(content.value)
            processTextWithOffsets(element, consumer, unescapedText, offsets, content.startOffset)
        }
    }

    private val sigilTokenizer = object : Tokenizer<PsiElement>() {
        override fun tokenize(@NotNull element: PsiElement, @NotNull consumer: TokenConsumer) {
            val text = element.text
            val content = extractSigilContent(text) ?: return

            if (content.sigilLetter == 'r' || content.sigilLetter == 'R') return
            if (content.value.isEmpty()) return

            consumer.consumeToken(
                element,
                content.value,
                false,
                content.startOffset,
                TextRange.allOf(content.value),
                PlainTextSplitter.getInstance()
            )
        }
    }

    override fun getTokenizer(element: PsiElement): Tokenizer<*> {
        val elementType = element.node?.elementType

        return when {
            // Enable spell checking for comments
            element is PsiComment -> super.getTokenizer(element)

            // Enable spell checking for string and charlist parts
            elementType == EX_STRING_PART || elementType == EX_CHARLIST_PART ->
                if (isRegexSigilPart(element)) EMPTY_TOKENIZER else stringTokenizer

            // Enable spell checking for text sigils (but not regex sigils)
            elementType?.toString() == "SIGIL" -> sigilTokenizer

            // Disable spell checking for everything else (identifiers, atoms, keywords, etc.)
            else -> EMPTY_TOKENIZER
        }
    }

    private data class StringContent(val value: String, val startOffset: Int)

    private data class SigilContent(val value: String, val startOffset: Int, val sigilLetter: Char)

    private fun extractStringContent(text: String): StringContent? = when {
        text.startsWith("\"\"\"") && text.endsWith("\"\"\"") && text.length >= 6 ->
            StringContent(text.substring(3, text.length - 3), 3)
        text.startsWith("'''") && text.endsWith("'''") && text.length >= 6 ->
            StringContent(text.substring(3, text.length - 3), 3)
        text.startsWith('"') && text.endsWith('"') && text.length >= 2 ->
            StringContent(text.substring(1, text.length - 1), 1)
        text.startsWith('\'') && text.endsWith('\'') && text.length >= 2 ->
            StringContent(text.substring(1, text.length - 1), 1)
        else -> StringContent(text, 0)
    }

    private fun extractSigilContent(text: String): SigilContent? {
        if (text.length < 3 || text[0] != '~') return null
        val sigilLetter = text[1]
        val delimiter = text[2]
        val closingDelimiter = when (delimiter) {
            '(' -> ')'
            '[' -> ']'
            '{' -> '}'
            '<' -> '>'
            else -> delimiter
        }

        var endIndex = text.length - 1
        while (endIndex > 2 && text[endIndex].isLetter()) {
            endIndex--
        }
        if (endIndex <= 2 || text[endIndex] != closingDelimiter) return null

        return SigilContent(text.substring(3, endIndex), 3, sigilLetter)
    }

    private fun isRegexSigilPart(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            val firstChildText = current.firstChild?.text
            if (firstChildText != null && (firstChildText.startsWith("~r") || firstChildText.startsWith("~R"))) {
                return true
            }
            current = current.parent
        }
        return false
    }

    private fun parseEscapes(text: String): Pair<StringBuilder, IntArray> {
        val out = StringBuilder(text.length)
        val offsets = IntArray(text.length + 1)
        var rawIndex = 0
        var outIndex = 0
        offsets[0] = 0

        while (rawIndex < text.length) {
            val current = text[rawIndex]
            if (current != '\\') {
                outIndex = appendChar(out, offsets, outIndex, current, rawIndex, rawIndex + 1)
                rawIndex++
                continue
            }

            val escapeStart = rawIndex
            rawIndex++
            if (rawIndex >= text.length) {
                outIndex = appendChar(out, offsets, outIndex, '\\', escapeStart, rawIndex)
                break
            }

            when (val next = text[rawIndex]) {
                '\n' -> rawIndex++
                '\r' -> {
                    rawIndex++
                    if (rawIndex < text.length && text[rawIndex] == '\n') rawIndex++
                }
                'x' -> {
                    val result = parseHexEscape(text, escapeStart, rawIndex, out, offsets, outIndex)
                    outIndex = result.outIndex
                    rawIndex = result.rawIndex
                }
                'u' -> {
                    val result = parseUnicodeEscape(text, escapeStart, rawIndex, out, offsets, outIndex)
                    outIndex = result.outIndex
                    rawIndex = result.rawIndex
                }
                else -> {
                    val mapped = mapSimpleEscape(next) ?: next
                    rawIndex++
                    outIndex = appendChar(out, offsets, outIndex, mapped, escapeStart, rawIndex)
                }
            }

            offsets[outIndex] = rawIndex
        }

        offsets[outIndex] = rawIndex
        return out to offsets
    }

    private data class EscapeParseResult(val outIndex: Int, val rawIndex: Int)

    private fun parseHexEscape(
        text: String,
        escapeStart: Int,
        rawIndex: Int,
        out: StringBuilder,
        offsets: IntArray,
        outIndex: Int
    ): EscapeParseResult {
        val nextIndex = rawIndex + 1
        if (nextIndex < text.length && text[nextIndex] == '{') {
            val (codePoint, endIndex) = parseBracedHex(text, nextIndex + 1)
            if (codePoint != null && endIndex != null) {
                val newOutIndex = appendCodePoint(out, offsets, outIndex, codePoint, escapeStart, endIndex + 1)
                return EscapeParseResult(newOutIndex, endIndex + 1)
            }
        } else if (nextIndex < text.length && text[nextIndex].isHexDigit()) {
            var end = nextIndex + 1
            if (end < text.length && text[end].isHexDigit()) end++
            val codePoint = text.substring(nextIndex, end).toInt(16)
            val newOutIndex = appendCodePoint(out, offsets, outIndex, codePoint, escapeStart, end)
            return EscapeParseResult(newOutIndex, end)
        }

        val newOutIndex = appendChar(out, offsets, outIndex, 'x', escapeStart, rawIndex + 1)
        return EscapeParseResult(newOutIndex, rawIndex + 1)
    }

    private fun parseUnicodeEscape(
        text: String,
        escapeStart: Int,
        rawIndex: Int,
        out: StringBuilder,
        offsets: IntArray,
        outIndex: Int
    ): EscapeParseResult {
        val nextIndex = rawIndex + 1
        if (nextIndex < text.length && text[nextIndex] == '{') {
            val (codePoint, endIndex) = parseBracedHex(text, nextIndex + 1)
            if (codePoint != null && endIndex != null) {
                val newOutIndex = appendCodePoint(out, offsets, outIndex, codePoint, escapeStart, endIndex + 1)
                return EscapeParseResult(newOutIndex, endIndex + 1)
            }
        } else if (nextIndex + 3 < text.length) {
            val hex = text.substring(nextIndex, nextIndex + 4)
            if (hex.all { it.isHexDigit() }) {
                val codePoint = hex.toInt(16)
                val newOutIndex = appendCodePoint(out, offsets, outIndex, codePoint, escapeStart, nextIndex + 4)
                return EscapeParseResult(newOutIndex, nextIndex + 4)
            }
        }

        val newOutIndex = appendChar(out, offsets, outIndex, 'u', escapeStart, rawIndex + 1)
        return EscapeParseResult(newOutIndex, rawIndex + 1)
    }

    private fun parseBracedHex(text: String, startIndex: Int): Pair<Int?, Int?> {
        var index = startIndex
        var digits = 0
        while (index < text.length && text[index].isHexDigit() && digits < 6) {
            index++
            digits++
        }
        if (digits == 0 || index >= text.length || text[index] != '}') return null to null
        val codePoint = text.substring(startIndex, index).toInt(16)
        return codePoint to index
    }

    private fun appendChar(
        out: StringBuilder,
        offsets: IntArray,
        outIndex: Int,
        value: Char,
        sourceStart: Int,
        sourceEnd: Int
    ): Int {
        out.append(value)
        offsets[outIndex] = sourceStart
        offsets[outIndex + 1] = sourceEnd
        return outIndex + 1
    }

    private fun appendCodePoint(
        out: StringBuilder,
        offsets: IntArray,
        outIndex: Int,
        codePoint: Int,
        sourceStart: Int,
        sourceEnd: Int
    ): Int {
        val chars = if (codePoint in 0..0x10FFFF) Character.toChars(codePoint) else charArrayOf('\uFFFD')
        var index = outIndex
        for (char in chars) {
            out.append(char)
            offsets[index] = sourceStart
            index++
            offsets[index] = sourceEnd
        }
        return index
    }

    private fun mapSimpleEscape(value: Char): Char? = when (value) {
        '0' -> '\u0000'
        'a' -> '\u0007'
        'b' -> '\b'
        'd' -> '\u007F'
        'e' -> '\u001B'
        'f' -> '\u000C'
        'n' -> '\n'
        'r' -> '\r'
        's' -> ' '
        't' -> '\t'
        'v' -> '\u000B'
        '\\' -> '\\'
        '"' -> '"'
        '\'' -> '\''
        else -> null
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'
}
