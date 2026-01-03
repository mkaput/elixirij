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
import com.intellij.psi.TokenType

/**
 * Provides quote handling for Elixir strings, charlists, heredocs, and sigils.
 *
 * Handles intelligent quote character pairing and literal detection for:
 * - Double-quoted strings: "..."
 * - Single-quoted charlists: '...'
 * - Heredocs: \"\"\" and '''
 * - Quoted atoms: :"..." and :'...'
 * - Sigils: ~r/.../, ~s"...", etc.
 *
 * Note: Auto-completion for heredocs and sigil delimiters is handled by
 * [ExTypedHandlerDelegate] since these require multi-character handling.
 * This handler primarily assists with detecting when the caret is inside
 * a string literal for proper quote behavior.
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
        super.isOpeningQuote(iterator, offset) || isQuoteAt(iterator, offset)

    override fun isClosingQuote(iterator: HighlighterIterator, offset: Int): Boolean =
        super.isClosingQuote(iterator, offset)

    override fun hasNonClosedLiteral(editor: Editor, iterator: HighlighterIterator, offset: Int): Boolean {
        if (super.hasNonClosedLiteral(editor, iterator, offset)) return true
        if (myLiteralTokenSet.contains(iterator.tokenType) && iterator.start == offset) {
            val document = editor.document
            val lineEnd = document.getLineEndOffset(document.getLineNumber(offset))
            if (iterator.end > lineEnd) return true
        }
        val tokenType = iterator.tokenType
        return (tokenType == TokenType.WHITE_SPACE || tokenType == BAD_CHARACTER) &&
            isQuoteAt(editor.document.charsSequence, offset)
    }

    override fun getClosingQuote(iterator: HighlighterIterator, offset: Int): CharSequence? = null

    private fun isQuoteAt(iterator: HighlighterIterator, offset: Int): Boolean {
        val document = iterator.document ?: return false
        return isQuoteAt(document.charsSequence, offset)
    }

    private fun isQuoteAt(chars: CharSequence, offset: Int): Boolean =
        offset in chars.indices && (chars[offset] == '"' || chars[offset] == '\'')
}
