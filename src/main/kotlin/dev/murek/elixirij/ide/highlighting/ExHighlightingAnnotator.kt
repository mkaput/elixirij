package dev.murek.elixirij.ide.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import dev.murek.elixirij.lang.EX_IDENTIFIER
import dev.murek.elixirij.lang.psi.*

/**
 * Annotator that provides semantic syntax highlighting based on parser information.
 *
 * This annotator highlights:
 * - Special form keywords (defmodule, def, case, etc.) that are parsed as identifiers
 * - Function/macro declaration names
 * - Module attribute names
 */
class ExHighlightingAnnotator : Annotator {

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (holder.isBatchMode) return

        when (element) {
            is ExDefmoduleExpr -> highlightDefmodule(element, holder)
            is ExDefExpr -> highlightDef(element, holder)
            is ExDefmacroExpr -> highlightDefmacro(element, holder)
            is ExDefguardExpr -> highlightDefguard(element, holder)
            is ExDefstructExpr -> highlightLeadingKeyword(element, holder)
            is ExDefexceptionExpr -> highlightLeadingKeyword(element, holder)
            is ExDefprotocolExpr -> highlightDefprotocol(element, holder)
            is ExDefimplExpr -> highlightDefimpl(element, holder)
            is ExImportExpr -> highlightLeadingKeyword(element, holder)
            is ExRequireExpr -> highlightLeadingKeyword(element, holder)
            is ExUseExpr -> highlightLeadingKeyword(element, holder)
            is ExAliasDirective -> highlightLeadingKeyword(element, holder)
            is ExForExpr -> highlightLeadingKeyword(element, holder)
            is ExQuoteExpr -> highlightLeadingKeyword(element, holder)
            is ExUnquoteExpr -> highlightLeadingKeyword(element, holder)
            is ExUnquoteSplicingExpr -> highlightLeadingKeyword(element, holder)
            is ExCaseExpr -> highlightLeadingKeyword(element, holder)
            is ExCondExpr -> highlightLeadingKeyword(element, holder)
            is ExWithExpr -> highlightLeadingKeyword(element, holder)
            is ExTryExpr -> highlightLeadingKeyword(element, holder)
            is ExReceiveExpr -> highlightLeadingKeyword(element, holder)
            is ExModuleAttr -> highlightModuleAttr(element, holder)
        }
    }

    private fun highlightDefmodule(element: ExDefmoduleExpr, holder: AnnotationHolder) {
        highlightLeadingKeyword(element, holder)
    }

    private fun highlightDef(element: ExDefExpr, holder: AnnotationHolder) {
        // Highlight the leading keyword (def/defp)
        highlightLeadingKeyword(element, holder)

        // Highlight the function name (first ExIdentifier child)
        val funcName = element.childrenOfType<ExIdentifier>().firstOrNull()
        if (funcName != null) {
            highlight(funcName, ExTextAttributes.FUNCTION_DECLARATION, holder)
        }
    }

    private fun highlightDefmacro(element: ExDefmacroExpr, holder: AnnotationHolder) {
        // Highlight the leading keyword (defmacro/defmacrop)
        highlightLeadingKeyword(element, holder)

        // Highlight the macro name (first ExIdentifier child)
        val macroName = element.childrenOfType<ExIdentifier>().firstOrNull()
        if (macroName != null) {
            highlight(macroName, ExTextAttributes.MACRO_DECLARATION, holder)
        }
    }

    private fun highlightDefguard(element: ExDefguardExpr, holder: AnnotationHolder) {
        // Highlight the leading keyword (defguard/defguardp)
        highlightLeadingKeyword(element, holder)

        // Highlight the guard name (first ExIdentifier child)
        val guardName = element.childrenOfType<ExIdentifier>().firstOrNull()
        if (guardName != null) {
            highlight(guardName, ExTextAttributes.MACRO_DECLARATION, holder)
        }
    }

    private fun highlightDefprotocol(element: ExDefprotocolExpr, holder: AnnotationHolder) {
        highlightLeadingKeyword(element, holder)
    }

    private fun highlightDefimpl(element: ExDefimplExpr, holder: AnnotationHolder) {
        highlightLeadingKeyword(element, holder)
    }

    private fun highlightModuleAttr(element: ExModuleAttr, holder: AnnotationHolder) {
        // The @ is already highlighted by the lexer-based highlighter.
        // Highlight the attribute name identifier (first ExIdentifier child).
        val attrName = element.childrenOfType<ExIdentifier>().firstOrNull()
        if (attrName != null) {
            highlight(attrName, ExTextAttributes.MODULE_ATTRIBUTE, holder)
        }
    }

    /**
     * Highlights the leading keyword token in a special form expression.
     *
     * Special forms like defmodule, def, case, etc. have their keyword as a bare
     * IDENTIFIER token (not wrapped in ExIdentifier). This finds that token and
     * highlights it as a keyword.
     */
    private fun highlightLeadingKeyword(element: PsiElement, holder: AnnotationHolder) {
        // The leading keyword is a bare IDENTIFIER token, not wrapped in ExIdentifier
        val keywordToken = element.node.getChildren(null)
            .firstOrNull { it.elementType == EX_IDENTIFIER }
            ?.psi

        if (keywordToken != null) {
            highlight(keywordToken, ExTextAttributes.SPECIAL_FORM, holder)
        }
    }

    private fun highlight(element: PsiElement, textAttributes: ExTextAttributes, holder: AnnotationHolder) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(element)
            .textAttributes(textAttributes.attribute)
            .create()
    }
}
