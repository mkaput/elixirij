package dev.murek.elixirij.ide.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.PsiTreeUtil
import dev.murek.elixirij.lang.psi.*

/**
 * Annotator that provides semantic syntax highlighting based on parser information.
 *
 * This annotator highlights:
 * - Special form keywords (defmodule, def, case, etc.) that are parsed as identifiers
 * - Function/macro declaration names
 * - Module attribute names
 */
class ExHighlightingAnnotator : Annotator, DumbAware {

    private val docAttributeNames = setOf("doc", "moduledoc")

    private val specialFormNames = setOf(
        "alias",
        "case",
        "cond",
        "def",
        "defexception",
        "defguard",
        "defguardp",
        "defimpl",
        "defmacro",
        "defmacrop",
        "defmodule",
        "defp",
        "defprotocol",
        "defstruct",
        "for",
        "if",
        "import",
        "quote",
        "raise",
        "receive",
        "require",
        "send",
        "super",
        "throw",
        "try",
        "unquote",
        "unquote_splicing",
        "unless",
        "use",
        "with"
    )

    private val functionDeclarationNames = setOf("def", "defp")
    private val macroDeclarationNames = setOf("defmacro", "defmacrop", "defguard", "defguardp")

    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        if (holder.isBatchMode) return

        when (element) {
            is ExModuleAttr -> highlightModuleAttr(element, holder)
            is ExNoParensCall -> highlightNoParensCall(element, holder)
            is ExBareDoCall -> highlightBareDoCall(element, holder)
            is ExAccessExpr -> highlightParenCall(element, holder)
            is ExIdentifier -> highlightUnusedIdentifier(element, holder)
        }
    }

    private fun highlightModuleAttr(element: ExModuleAttr, holder: AnnotationHolder) {
        // The @ is already highlighted by the lexer-based highlighter.
        // Highlight the attribute name identifier (first ExIdentifier child).
        val attrName = element.childrenOfType<ExIdentifier>().firstOrNull()
        if (attrName != null) {
            if (attrName.text in docAttributeNames) {
                highlight(element, ExTextAttributes.DOC_COMMENT, holder)
            } else {
                highlight(attrName, ExTextAttributes.MODULE_ATTRIBUTE, holder)
            }
        }
    }

    private fun highlightNoParensCall(element: ExNoParensCall, holder: AnnotationHolder) {
        val callTarget = element.children.firstOrNull { it is ExIdentifier } as? ExIdentifier ?: return
        highlightSpecialForm(callTarget, holder)
        highlightDeclarationName(callTarget.text, element, callTarget, holder)
    }

    private fun highlightBareDoCall(element: ExBareDoCall, holder: AnnotationHolder) {
        val callTarget = element.children.firstOrNull { it is ExIdentifier } as? ExIdentifier ?: return
        highlightSpecialForm(callTarget, holder)
    }

    private fun highlightParenCall(element: ExAccessExpr, holder: AnnotationHolder) {
        if (!element.children.any { it is ExParenCall || it is ExAnonymousCall }) return
        val callTarget = element.children.firstOrNull { it is ExIdentifier } as? ExIdentifier ?: return
        highlightSpecialForm(callTarget, holder)
    }

    private fun highlightSpecialForm(callTarget: ExIdentifier, holder: AnnotationHolder) {
        if (callTarget.text in specialFormNames) {
            highlight(callTarget, ExTextAttributes.SPECIAL_FORM, holder)
        }
    }

    private fun highlightDeclarationName(
        callName: String, element: ExNoParensCall, callTarget: ExIdentifier, holder: AnnotationHolder
    ) {
        val declarationAttribute = when (callName) {
            in functionDeclarationNames -> ExTextAttributes.FUNCTION_DECLARATION
            in macroDeclarationNames -> ExTextAttributes.MACRO_DECLARATION
            else -> null
        } ?: return
        val declarationName = findDeclarationName(element, callTarget) ?: return
        highlight(declarationName, declarationAttribute, holder)
    }

    private fun highlightUnusedIdentifier(element: ExIdentifier, holder: AnnotationHolder) {
        val name = element.text
        if (name.startsWith("_") && !isAllCapsIdentifier(name)) {
            highlight(element, ExTextAttributes.UNUSED_VARIABLE, holder)
        }
    }

    private fun isAllCapsIdentifier(name: String): Boolean {
        val hasLetter = name.any { it.isLetter() }
        val allLettersUpper = name.all { !it.isLetter() || it.isUpperCase() }
        return hasLetter && allLettersUpper
    }

    private fun findDeclarationName(element: ExNoParensCall, callTarget: ExIdentifier): PsiElement? {
        val doBlock = PsiTreeUtil.findChildOfType(element, ExDoBlock::class.java)
        val limitOffset = doBlock?.textRange?.startOffset ?: Int.MAX_VALUE
        var leaf = PsiTreeUtil.nextLeaf(callTarget, true)
        while (leaf != null && leaf.textRange.startOffset < limitOffset) {
            when (val parent = leaf.parent) {
                is ExIdentifier, is ExAtom -> if (parent != callTarget) return parent
            }
            leaf = PsiTreeUtil.nextLeaf(leaf, true)
        }
        return null
    }

    private fun highlight(element: PsiElement, textAttributes: ExTextAttributes, holder: AnnotationHolder) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(element)
            .textAttributes(textAttributes.attribute).create()
    }
}
