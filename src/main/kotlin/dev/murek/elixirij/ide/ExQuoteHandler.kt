package dev.murek.elixirij.ide

import com.intellij.codeInsight.editorActions.MultiCharQuoteHandler
import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.highlighter.HighlighterIterator
import dev.murek.elixirij.lang.EX_ATOM_QUOTED
import dev.murek.elixirij.lang.EX_CHARLIST
import dev.murek.elixirij.lang.EX_CHARLIST_HEREDOC
import dev.murek.elixirij.lang.EX_HEREDOC
import dev.murek.elixirij.lang.EX_SIGIL
import dev.murek.elixirij.lang.EX_STRING
import dev.murek.elixirij.lang.BAD_CHARACTER

/**
 * Provides quote handling for Elixir strings, charlists, heredocs, and sigils.
 *
 * Handles intelligent quote character pairing and literal detection for:
 * - Double-quoted strings: "..."
 * - Single-quoted charlists: '...'
 * - Heredocs: \"\"\" and '''
 * - Quoted atoms: :"..." and :'...'
 * - Sigils: ~r/.../, ~s"...", etc.
 */
class ExQuoteHandler : SimpleTokenSetQuoteHandler(
    EX_STRING,
    EX_CHARLIST,
    EX_HEREDOC,
    EX_CHARLIST_HEREDOC,
    EX_SIGIL,
    EX_ATOM_QUOTED
), MultiCharQuoteHandler {

    override fun isOpeningQuote(iterator: HighlighterIterator, offset: Int): Boolean =
        super.isOpeningQuote(iterator, offset) || isDanglingQuote(iterator, offset)

    override fun isClosingQuote(iterator: HighlighterIterator, offset: Int): Boolean =
        super.isClosingQuote(iterator, offset) || isDanglingQuote(iterator, offset)

    override fun hasNonClosedLiteral(editor: Editor, iterator: HighlighterIterator, offset: Int): Boolean =
        isDanglingQuote(editor, iterator, offset) || super.hasNonClosedLiteral(editor, iterator, offset)

    override fun getClosingQuote(iterator: HighlighterIterator, offset: Int): CharSequence? = null

    private fun isDanglingQuote(iterator: HighlighterIterator, offset: Int): Boolean =
        iterator.tokenType == BAD_CHARACTER && isQuoteAt(iterator, offset)

    private fun isDanglingQuote(editor: Editor, iterator: HighlighterIterator, offset: Int): Boolean =
        iterator.tokenType == BAD_CHARACTER && isQuoteAt(editor.document.charsSequence, offset)

    private fun isQuoteAt(iterator: HighlighterIterator, offset: Int): Boolean {
        val document = iterator.document ?: return false
        return isQuoteAt(document.charsSequence, offset)
    }

    private fun isQuoteAt(chars: CharSequence, offset: Int): Boolean =
        offset in chars.indices && (chars[offset] == '"' || chars[offset] == '\'')
}
