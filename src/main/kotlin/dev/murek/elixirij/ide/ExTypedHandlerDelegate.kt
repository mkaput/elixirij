package dev.murek.elixirij.ide

import com.intellij.codeInsight.editorActions.TypedHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorModificationUtil
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import dev.murek.elixirij.ExLanguage
import dev.murek.elixirij.lang.EX_INTERPOLATION_START

/**
 * Provides smart typing behavior for Elixir-specific delimiters.
 *
 * Features:
 * - Heredoc completion: """ and ''' insert closing heredoc with newline
 * - Sigil delimiter auto-pairing: ~r[, ~s(, etc.
 * - Overtype closing delimiters instead of inserting duplicates
 *
 * Note: Standard delimiter auto-pairing for (), [], {}, and quotes is handled
 * by the platform through ExBraceMatcher and ExQuoteHandler.
 */
class ExTypedHandlerDelegate : TypedHandlerDelegate() {

    /** Maps sigil opening delimiters to their closing counterparts */
    private val sigilDelimiters = mapOf(
        '(' to ')',
        '[' to ']',
        '{' to '}',
        '<' to '>',
        '/' to '/',
        '|' to '|',
        '"' to '"',
        '\'' to '\''
    )

    override fun beforeCharTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile,
        fileType: FileType
    ): Result {
        if (file.language != ExLanguage) return Result.CONTINUE

        val offset = editor.caretModel.offset
        val document = editor.document
        val text = document.charsSequence

        // Handle heredoc completion: """ and '''
        // We handle this in beforeCharTyped because the QuoteHandler may interfere
        // Check if we're about to type the third quote
        if ((c == '"' || c == '\'') && offset >= 2) {
            val prevTwo = text.subSequence(offset - 2, offset).toString()
            val expectedPrevTwo = if (c == '"') "\"\"" else "''"

            if (prevTwo == expectedPrevTwo) {
                val heredocOpener = if (c == '"') "\"\"\"" else "'''"
                // Check if there's already a closing heredoc ahead (avoid duplicate insertion)
                val remaining = text.subSequence(offset, text.length)
                if (!remaining.contains(heredocOpener)) {
                    // Insert the third quote + closing heredoc (no newline)
                    EditorModificationUtil.insertStringAtCaret(editor, "$c$heredocOpener", false, false)
                    // Place caret right after the opening heredoc
                    editor.caretModel.moveToOffset(offset + 1)
                    return Result.STOP
                }
            }
        }

        // Handle overtype for closing delimiters
        if (offset < document.textLength) {
            val charAhead = text[offset]

            // Overtype closing delimiter if it matches what we're typing
            if (shouldOvertype(c, charAhead)) {
                EditorModificationUtil.moveCaretRelatively(editor, 1)
                return Result.STOP
            }
        }

        return Result.CONTINUE
    }

    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        if (file.language != ExLanguage) return Result.CONTINUE

        val offset = editor.caretModel.offset
        val text = editor.document.charsSequence

        // Handle sigil delimiter auto-pairing: ~r[, ~s(, etc.
        // After typing the delimiter, offset points after it, so we check chars before the delimiter
        if (c in sigilDelimiters.keys && offset >= 3) {
            // Check the two chars before the delimiter we just typed (offset-1 is where delimiter is)
            val sigilPrefix = text.subSequence(offset - 3, offset - 1).toString()
            // Check for sigil pattern: ~[a-zA-Z]
            if (sigilPrefix.length == 2 && sigilPrefix[0] == '~' && sigilPrefix[1].isLetter()) {
                val closingDelimiter = sigilDelimiters[c]!!

                // Don't auto-pair if next char is alphanumeric or the same closing delimiter
                if (offset < text.length) {
                    val nextChar = text[offset]
                    if (nextChar.isLetterOrDigit() || nextChar == closingDelimiter) {
                        return Result.CONTINUE
                    }
                }

                // For heredoc sigils (""" or '''), handle separately
                if (c == '"' || c == '\'') {
                    // Let the heredoc handler deal with triple quotes
                    return Result.CONTINUE
                }

                val openingOffset = when {
                    offset < text.length && text[offset] == c -> offset
                    text[offset - 1] == c -> offset - 1
                    else -> null
                } ?: return Result.CONTINUE

                val insertOffset = openingOffset + 1
                editor.caretModel.moveToOffset(insertOffset)
                EditorModificationUtil.insertStringAtCaret(editor, closingDelimiter.toString(), false, false)
                editor.caretModel.moveToOffset(insertOffset)
                return Result.STOP
            }
        }

        if (c == '{' && offset >= 2 && isInterpolationStart(file, offset)) {
            if (offset < text.length && text[offset] == '}') {
                return Result.CONTINUE
            }
            EditorModificationUtil.insertStringAtCaret(editor, "}", false, false)
            return Result.STOP
        }

        return Result.CONTINUE
    }

    /**
     * Determines if we should overtype the character ahead instead of inserting a new one.
     */
    private fun shouldOvertype(typed: Char, charAhead: Char): Boolean =
        typed == charAhead && typed in setOf(')', ']', '}', '"', '\'')

    private fun isInterpolationStart(file: PsiFile, offset: Int): Boolean {
        val element = file.findElementAt(offset - 1) ?: return false
        return element.node.elementType == EX_INTERPOLATION_START
    }

}
