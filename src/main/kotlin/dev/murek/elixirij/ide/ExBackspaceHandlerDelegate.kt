package dev.murek.elixirij.ide

import com.intellij.codeInsight.editorActions.BackspaceHandlerDelegate
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import dev.murek.elixirij.ExLanguage
import dev.murek.elixirij.lang.EX_INTERPOLATION_START

/**
 * Removes paired interpolation braces when backspacing inside "#{...}".
 */
class ExBackspaceHandlerDelegate : BackspaceHandlerDelegate() {

    override fun beforeCharDeleted(c: Char, file: PsiFile, editor: Editor) {
        if (file.language != ExLanguage || c != '{') return

        val offset = editor.caretModel.offset
        val text = editor.document.charsSequence
        if (offset < 2 || offset >= text.length) return
        if (text[offset] != '}' || text[offset - 2] != '#') return

        val element = file.findElementAt(offset - 1) ?: return
        if (element.node.elementType != EX_INTERPOLATION_START) return

        editor.document.deleteString(offset, offset + 1)
    }

    override fun charDeleted(c: Char, file: PsiFile, editor: Editor): Boolean = false
}
