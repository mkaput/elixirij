package dev.murek.elixirij.ide.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiUtilCore
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.PsiTreeUtil
import dev.murek.elixirij.lang.EX_DOT
import dev.murek.elixirij.lang.EX_DO
import dev.murek.elixirij.lang.EX_LBRACKET
import dev.murek.elixirij.lang.EX_LPAREN
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
        "__CALLER__",
        "__DIR__",
        "__ENV__",
        "__MODULE__",
        "__STACKTRACE__",
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
            is ExCallExpr -> highlightCall(element, holder)
            is ExDotAccess -> highlightDotAccessCall(element, holder)
            is ExParenExpr -> highlightCallBeforeParen(element, holder)
            is ExIdentifier -> {
                highlightSpecialForm(element, holder)
                highlightQualifiedCallElement(element, holder)
                highlightCallLikeElement(element, holder)
                highlightUnusedIdentifier(element, holder)
            }
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
            if (attrName.text == "spec") {
                highlightSpecQualifiedCalls(element, holder)
            }
        }
    }

    private fun highlightCall(element: ExCallExpr, holder: AnnotationHolder) {
        if (isInsideSpec(element)) return
        val specialFormTarget = findSpecialFormTarget(element)
        if (specialFormTarget != null) {
            highlightSpecialForm(specialFormTarget, holder)
            highlightDeclarationName(specialFormTarget.text, element, specialFormTarget, holder)
        }
        highlightFunctionCall(element, holder)
        val skipTarget = findFunctionCallTarget(element)?.element
        highlightQualifiedCallsInRange(element, holder, skipTarget)
        highlightNestedCallIdentifiers(element, holder, skipTarget)
    }

    private fun highlightSpecialForm(callTarget: ExIdentifier, holder: AnnotationHolder) {
        if (callTarget.text in specialFormNames) {
            highlight(callTarget, ExTextAttributes.SPECIAL_FORM, holder)
        }
    }

    private fun highlightDeclarationName(
        callName: String, element: ExCallExpr, callTarget: ExIdentifier, holder: AnnotationHolder
    ) {
        val declarationAttribute = when (callName) {
            in functionDeclarationNames -> ExTextAttributes.FUNCTION_DECLARATION
            in macroDeclarationNames -> ExTextAttributes.MACRO_DECLARATION
            else -> null
        } ?: return
        val declarationName = findDeclarationName(element, callTarget) ?: return
        highlight(declarationName, declarationAttribute, holder)
    }

    private fun findSpecialFormTarget(element: ExCallExpr): ExIdentifier? {
        val children = element.children
        val firstIdentifier = children.firstOrNull { it is ExIdentifier } as? ExIdentifier ?: return null
        if (children.any { it is ExDotAccess }) return null
        return firstIdentifier
    }

    private fun highlightFunctionCall(element: ExCallExpr, holder: AnnotationHolder) {
        val target = findFunctionCallTarget(element) ?: return
        if (!target.isQualified && target.element.text in specialFormNames) return
        if (isDeclarationNameTarget(target.element)) return
        if (target.element !is ExIdentifier) return
        highlight(target.element, ExTextAttributes.FUNCTION_CALL, holder)
    }

    private fun highlightQualifiedCallElement(element: ExIdentifier, holder: AnnotationHolder) {
        if (isInsideSpec(element)) return
        val callExpr = PsiTreeUtil.getParentOfType(element, ExCallExpr::class.java)
        if (callExpr != null) {
            val callTarget = findFunctionCallTarget(callExpr)
            if (callTarget?.element == element) return
        }
        val prevLeaf = previousNonWhitespaceLeaf(element) ?: return
        if (PsiUtilCore.getElementType(prevLeaf) != EX_DOT) return
        val nextLeaf = nextNonWhitespaceLeaf(element) ?: return
        if (PsiUtilCore.getElementType(nextLeaf) != EX_LPAREN) return
        if (isDeclarationNameTarget(element)) return
        highlight(element, ExTextAttributes.FUNCTION_CALL, holder)
    }

    private fun highlightCallLikeElement(element: ExIdentifier, holder: AnnotationHolder) {
        if (isDeclarationNameTarget(element)) return
        if (element is ExIdentifier && element.text in specialFormNames) return
        val nextLeaf = nextNonWhitespaceLeaf(element) ?: return
        if (PsiUtilCore.getElementType(nextLeaf) != EX_LPAREN) return
        highlight(element, ExTextAttributes.FUNCTION_CALL, holder)
    }

    private fun highlightNestedCallIdentifiers(
        element: ExCallExpr,
        holder: AnnotationHolder,
        skipTarget: PsiElement?
    ) {
        val identifiers = PsiTreeUtil.findChildrenOfType(element, ExIdentifier::class.java)
        for (identifier in identifiers) {
            if (identifier.text in specialFormNames) continue
            val nextLeaf = nextNonWhitespaceLeaf(identifier) ?: continue
            if (PsiUtilCore.getElementType(nextLeaf) != EX_LPAREN) continue
            if (isDeclarationNameTarget(identifier)) continue
            if (skipTarget == null || identifier != skipTarget) {
                highlight(identifier, ExTextAttributes.FUNCTION_CALL, holder)
            }
        }
    }

    private fun highlightDotAccessCall(element: ExDotAccess, holder: AnnotationHolder) {
        if (isInsideSpec(element)) return
        val target = element.children.lastOrNull { it is ExIdentifier } ?: return
        val nextLeaf = nextNonWhitespaceLeaf(target) ?: return
        if (PsiUtilCore.getElementType(nextLeaf) != EX_LPAREN) return
        if (isDeclarationNameTarget(target)) return
        highlight(target, ExTextAttributes.FUNCTION_CALL, holder)
    }

    private fun highlightCallBeforeParen(element: ExParenExpr, holder: AnnotationHolder) {
        val prevLeaf = previousNonWhitespaceLeaf(element) ?: return
        val elementType = PsiUtilCore.getElementType(prevLeaf) ?: return
        if (elementType != dev.murek.elixirij.lang.EX_IDENTIFIER && elementType != dev.murek.elixirij.lang.EX_ATOM) return
        val target = prevLeaf.parent ?: prevLeaf
        if (!element.textRange.contains(target.textRange)) return
        if (isDeclarationNameTarget(target)) return
        val identifierTarget = target as? ExIdentifier ?: return
        if (identifierTarget.text in specialFormNames) return
        highlight(identifierTarget, ExTextAttributes.FUNCTION_CALL, holder)
    }


    private fun highlightSpecQualifiedCalls(element: ExModuleAttr, holder: AnnotationHolder) {
        highlightSpecCallsInRange(element, holder)
    }

    private fun highlightQualifiedCallsInRange(
        element: ExCallExpr,
        holder: AnnotationHolder,
        skipTarget: PsiElement?
    ) {
        val startOffset = element.textRange.startOffset
        val endOffset = element.textRange.endOffset
        var leaf: PsiElement? = PsiTreeUtil.getDeepestFirst(element)
        while (leaf != null && leaf.textRange.startOffset < endOffset) {
            if (leaf.textRange.endOffset <= startOffset) {
                leaf = PsiTreeUtil.nextLeaf(leaf, true)
                continue
            }
            val elementType = PsiUtilCore.getElementType(leaf)
            if (elementType == dev.murek.elixirij.lang.EX_IDENTIFIER || elementType == dev.murek.elixirij.lang.EX_ATOM) {
                val nextLeaf = nextNonWhitespaceLeaf(leaf)
                if (nextLeaf != null && PsiUtilCore.getElementType(nextLeaf) == EX_LPAREN) {
                    val target = leaf.parent as? ExIdentifier
                    if (target != null &&
                        !isDeclarationNameTarget(target) &&
                        (skipTarget == null || target != skipTarget)
                    ) {
                        highlight(target, ExTextAttributes.FUNCTION_CALL, holder)
                    }
                }
            }
            leaf = PsiTreeUtil.nextLeaf(leaf, true)
        }
    }

    private fun highlightSpecCallsInRange(element: ExModuleAttr, holder: AnnotationHolder) {
        val startOffset = element.textRange.startOffset
        val endOffset = element.textRange.endOffset
        var leaf: PsiElement? = PsiTreeUtil.getDeepestFirst(element)
        while (leaf != null && leaf.textRange.startOffset < endOffset) {
            if (leaf.textRange.endOffset <= startOffset) {
                leaf = PsiTreeUtil.nextLeaf(leaf, true)
                continue
            }
            val elementType = PsiUtilCore.getElementType(leaf)
            if (elementType == dev.murek.elixirij.lang.EX_IDENTIFIER || elementType == dev.murek.elixirij.lang.EX_ATOM) {
                val nextLeaf = nextNonWhitespaceLeaf(leaf)
                if (nextLeaf != null && PsiUtilCore.getElementType(nextLeaf) == EX_LPAREN) {
                    val target = leaf.parent as? ExIdentifier
                    if (target != null) {
                        highlight(target, ExTextAttributes.FUNCTION_CALL, holder)
                    }
                }
            }
            leaf = PsiTreeUtil.nextLeaf(leaf, true)
        }
    }

    private data class CallTarget(val element: PsiElement, val isQualified: Boolean)

    private fun findFunctionCallTarget(element: ExCallExpr): CallTarget? {
        val start = element.textRange.startOffset
        val end = element.textRange.endOffset
        var leaf: PsiElement? = PsiTreeUtil.getDeepestFirst(element)
        var lastTarget: PsiElement? = null
        var sawDot = false
        while (leaf != null && leaf.textRange.startOffset < end) {
            if (leaf.textRange.endOffset <= start) {
                leaf = PsiTreeUtil.nextLeaf(leaf, true)
                continue
            }
            when (PsiUtilCore.getElementType(leaf)) {
                EX_DOT -> {
                    val nextLeaf = nextNonWhitespaceLeaf(leaf) ?: return null
                    if (PsiUtilCore.getElementType(nextLeaf) == EX_LPAREN) return null
                    sawDot = true
                }

                EX_LPAREN, EX_DO -> return lastTarget?.let { CallTarget(it, sawDot) }
            }
            val parent = leaf.parent
            if (parent is ExIdentifier || parent is ExAtom) {
                lastTarget = parent
                val nextLeaf = nextNonWhitespaceLeaf(parent)
                val nextType = nextLeaf?.let { PsiUtilCore.getElementType(it) }
                if (nextType != EX_DOT && nextType != EX_LPAREN && nextType != EX_DO && nextType != EX_LBRACKET) {
                    return CallTarget(parent, sawDot)
                }
            }
            leaf = PsiTreeUtil.nextLeaf(leaf, true)
        }
        return lastTarget?.let { CallTarget(it, sawDot) }
    }

    private fun isDeclarationNameTarget(target: PsiElement): Boolean {
        var callExpr = PsiTreeUtil.getParentOfType(target, ExCallExpr::class.java)
        while (callExpr != null) {
            val callTarget = findSpecialFormTarget(callExpr)
            if (callTarget != null) {
                val callName = callTarget.text
                if (callName in functionDeclarationNames || callName in macroDeclarationNames) {
                    val declarationName = findDeclarationName(callExpr, callTarget)
                    if (declarationName != null && PsiTreeUtil.isAncestor(declarationName, target, false)) {
                        return true
                    }
                }
            }
            callExpr = PsiTreeUtil.getParentOfType(callExpr, ExCallExpr::class.java, true)
        }
        return false
    }

    private fun isInsideSpec(element: PsiElement): Boolean {
        val moduleAttr = PsiTreeUtil.getParentOfType(element, ExModuleAttr::class.java) ?: return false
        val attrName = moduleAttr.childrenOfType<ExIdentifier>().firstOrNull()?.text ?: return false
        return attrName == "spec"
    }

    private fun nextNonWhitespaceLeaf(element: PsiElement): PsiElement? {
        var leaf = PsiTreeUtil.nextLeaf(element, true)
        while (leaf != null && PsiUtilCore.getElementType(leaf) == TokenType.WHITE_SPACE) {
            leaf = PsiTreeUtil.nextLeaf(leaf, true)
        }
        return leaf
    }

    private fun previousNonWhitespaceLeaf(element: PsiElement): PsiElement? {
        var leaf = PsiTreeUtil.prevLeaf(element, true)
        while (leaf != null && PsiUtilCore.getElementType(leaf) == TokenType.WHITE_SPACE) {
            leaf = PsiTreeUtil.prevLeaf(leaf, true)
        }
        return leaf
    }

    private fun highlightUnusedIdentifier(element: ExIdentifier, holder: AnnotationHolder) {
        val name = element.text
        if (name.startsWith("_") && !isAllCapsIdentifier(name) && !isElixirUnderscoreForm(name)) {
            highlight(element, ExTextAttributes.UNUSED_VARIABLE, holder)
        }
    }

    private fun isAllCapsIdentifier(name: String): Boolean {
        val hasLetter = name.any { it.isLetter() }
        val allLettersUpper = name.all { !it.isLetter() || it.isUpperCase() }
        return hasLetter && allLettersUpper
    }

    private fun isElixirUnderscoreForm(name: String): Boolean =
        name.length > 4 && name.startsWith("__") && name.endsWith("__")

    private fun findDeclarationName(element: ExCallExpr, callTarget: ExIdentifier): PsiElement? {
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
