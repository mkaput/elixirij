package dev.murek.elixirij.ide.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.StringEscapesTokenTypes
import com.intellij.psi.PsiElement

class ExEscapeSequenceAnnotator : Annotator, DumbAware {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val type = element.node.elementType
        when (type) {
            StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN -> {
                holder.newAnnotation(HighlightSeverity.ERROR, "Invalid escape sequence")
                    .range(element)
                    .create()
            }
            StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN -> {
                holder.newAnnotation(HighlightSeverity.ERROR, "Invalid Unicode escape sequence")
                    .range(element)
                    .create()
            }
            StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN -> annotateInvalidCodePoint(element, holder)
        }
    }

    private fun annotateInvalidCodePoint(element: PsiElement, holder: AnnotationHolder) {
        val text = element.text
        if (!text.startsWith("\\u{") || !text.endsWith("}")) return

        val hex = text.substring(3, text.length - 1)
        val codePoint = hex.toIntOrNull(16) ?: return
        if (isValidUnicodeScalar(codePoint)) return

        holder.newAnnotation(HighlightSeverity.ERROR, "Invalid Unicode code point in escape sequence")
            .range(element)
            .create()
    }

    private fun isValidUnicodeScalar(codePoint: Int): Boolean {
        if (codePoint < 0 || codePoint > 0x10FFFF) return false
        if (codePoint in 0xD800..0xDFFF) return false
        if (codePoint in 0xFDD0..0xFDEF) return false
        if (codePoint and 0xFFFF == 0xFFFE || codePoint and 0xFFFF == 0xFFFF) return false
        return true
    }
}
