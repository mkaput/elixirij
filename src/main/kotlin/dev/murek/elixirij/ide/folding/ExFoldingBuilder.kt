package dev.murek.elixirij.ide.folding

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilderEx
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import dev.murek.elixirij.lang.EX_ARROW
import dev.murek.elixirij.lang.EX_PIPE
import dev.murek.elixirij.lang.psi.*

class ExFoldingBuilder : FoldingBuilderEx(), DumbAware {
    override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
        val descriptors = ArrayList<FoldingDescriptor>()

        root.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (isFoldable(element) && shouldFold(element, document)) {
                    descriptors.add(FoldingDescriptor(element.node, element.textRange))
                }
                super.visitElement(element)
            }
        })

        return descriptors.toTypedArray()
    }

    override fun getPlaceholderText(node: ASTNode): String? = when (val element = node.psi) {
        is ExList -> listPlaceholder(element)
        is ExMap -> mapPlaceholder(element)
        is ExStruct -> structPlaceholder(element)
        is ExTuple -> tuplePlaceholder(element)
        is ExBitstring -> bitstringPlaceholder(element)
        is ExInterpolatedString -> interpolatedStringPlaceholder(element)
        is ExInterpolatedCharlist -> interpolatedCharlistPlaceholder(element)
        is ExHeredoc -> heredocPlaceholder("\"\"\"", element)
        is ExCharlistHeredoc -> heredocPlaceholder("'''", element)
        is ExSigil -> sigilPlaceholder(element.text)
        is ExDoBlock -> doBlockPlaceholder(element)
        is ExFnExpr -> fnBlockPlaceholder(element)
        else -> null
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    private fun isFoldable(element: PsiElement): Boolean = when (element) {
        is ExList,
        is ExMap,
        is ExStruct,
        is ExTuple,
        is ExBitstring,
        is ExInterpolatedString,
        is ExInterpolatedCharlist,
        is ExHeredoc,
        is ExCharlistHeredoc,
        is ExSigil,
        is ExDoBlock,
        is ExFnExpr -> true

        else -> false
    }

    private fun shouldFold(element: PsiElement, document: Document): Boolean {
        val range = element.textRange
        return !range.isEmpty && isMultiline(range, document)
    }

    private fun isMultiline(range: TextRange, document: Document): Boolean =
        document.getLineNumber(range.startOffset) < document.getLineNumber(range.endOffset)

    private fun bracketPlaceholder(
        prefix: String,
        suffix: String,
        element: PsiElement,
        skipFirstLine: Boolean
    ): String {
        val inline = inlineResult(element, skipFirstLine, suffix)
        val content = inline.text ?: "..."
        val body = if (inline.hasMore) {
            if (inline.endsWithComma) {
                "${content.trimEnd(',')}, ..."
            } else {
                "$content ..."
            }
        } else {
            content
        }
        return "$prefix$body$suffix"
    }

    private fun listPlaceholder(element: ExList): String {
        if (hasToken(element, EX_PIPE)) return "[...]"
        val expressions = element.expressionList
        val isSimple = expressions.size == 1 && isSimpleExpression(expressions.single())
        return if (isSimple) {
            bracketPlaceholder("[", "]", element, skipFirstLine = true)
        } else {
            "[...]"
        }
    }

    private fun tuplePlaceholder(element: ExTuple): String {
        val expressions = element.expressionList
        val isSimple = expressions.size == 1 && isSimpleExpression(expressions.single())
        return if (isSimple) {
            bracketPlaceholder("{", "}", element, skipFirstLine = true)
        } else {
            "{...}"
        }
    }

    private fun mapPlaceholder(element: ExMap): String {
        return "%{...}"
    }

    private fun structPlaceholder(element: ExStruct): String {
        val prefix = element.text.substringBefore("{").trim()
        return "$prefix{...}"
    }

    private fun bitstringPlaceholder(element: ExBitstring): String {
        val expressions = element.expressionList
        val isSimple = expressions.size == 1 && isSimpleExpression(expressions.single())
        return if (isSimple) {
            bracketPlaceholder("<<", ">>", element, skipFirstLine = true)
        } else {
            "<<...>>"
        }
    }

    private fun heredocPlaceholder(delimiter: String, element: PsiElement): String {
        val inline = inlineResult(element, skipFirstLine = true, closingDelimiter = delimiter)
        val content = inline.text ?: "..."
        val ellipsis = if (inline.hasMore) " ..." else ""
        return "$delimiter$content$ellipsis$delimiter"
    }

    private fun interpolatedStringPlaceholder(element: ExInterpolatedString): String {
        val text = element.text
        return when {
            text.startsWith("~") -> sigilPlaceholder(text)
            text.startsWith("\"\"\"") -> heredocPlaceholder("\"\"\"", element)
            else -> "\"...\""
        }
    }

    private fun interpolatedCharlistPlaceholder(element: ExInterpolatedCharlist): String {
        val text = element.text
        return when {
            text.startsWith("'''") -> heredocPlaceholder("'''", element)
            else -> "'...'"
        }
    }

    private fun doBlockPlaceholder(element: PsiElement): String {
        val expr = onlyDoBlockExpression(element)
        return if (expr == null) {
            "do ... end"
        } else if (isSimpleExpression(expr)) {
            "do ${expr.text.trim()} end"
        } else {
            "do ... end"
        }
    }

    private fun fnBlockPlaceholder(element: PsiElement): String {
        val lines = element.text.lineSequence().toList()
        val header = firstNonEmptyLine(lines)
        val bodyExpr = onlyFnBodyExpression(element)
        return when {
            header == null -> "fn ... end"
            bodyExpr == null -> "${header.trim()} ... end"
            isSimpleExpression(bodyExpr) -> "${header.trim()} ${bodyExpr.text.trim()} end"
            else -> "${header.trim()} ... end"
        }
    }

    private fun firstNonEmptyLine(lines: List<String>): String? =
        lines.firstOrNull { it.isNotBlank() }?.trim()

    private fun inlineResult(
        element: PsiElement,
        skipFirstLine: Boolean,
        closingDelimiter: String?
    ): InlineResult {
        val lines = element.text.lineSequence().toList()
        val candidates = if (skipFirstLine) lines.drop(1) else lines
        val contentLines = candidates.asSequence()
            .filter { it.isNotBlank() }
            .filterNot { closingDelimiter != null && it.trim() == closingDelimiter }
            .toList()
        val first = contentLines.firstOrNull()?.trim() ?: return InlineResult(null, false, false)
        val trimmed = first.trimEnd(',', ';').take(MAX_INLINE_LENGTH)
        val hasMore = contentLines.drop(1).isNotEmpty()
        val endsWithComma = first.trimEnd().endsWith(",")
        return InlineResult(trimmed.ifEmpty { null }, hasMore, endsWithComma)
    }

    private fun sigilPlaceholder(text: String): String {
        if (text.length < 3) return "~..."
        val prefix = text.take(2)
        val delimiter = sigilDelimiter(text.drop(2))
        val closing = sigilClosingDelimiter(delimiter)
        val lines = text.lineSequence().toList()
        val contentLines = lines.drop(1)
            .filter { it.isNotBlank() }
            .filterNot { line -> isSigilClosingLine(line, closing) }
        val first = contentLines.firstOrNull()?.trim()
        val trimmed = first?.trimEnd(',', ';')?.take(MAX_INLINE_LENGTH)
        val content = trimmed ?: "..."
        val ellipsis = if (contentLines.drop(1).any { it.isNotBlank() }) " ..." else ""
        return if (delimiter.isEmpty()) "$prefix$content$ellipsis" else "$prefix$delimiter$content$ellipsis$closing"
    }

    private fun sigilDelimiter(suffix: String): String = when {
        suffix.startsWith("\"\"\"") -> "\"\"\""
        suffix.startsWith("'''") -> "'''"
        suffix.isNotEmpty() -> suffix.first().toString()
        else -> ""
    }

    private fun sigilClosingDelimiter(delimiter: String): String = when (delimiter) {
        "(" -> ")"
        "[" -> "]"
        "{" -> "}"
        "<" -> ">"
        else -> delimiter
    }

    private fun isSigilClosingLine(line: String, closing: String): Boolean {
        val trimmed = line.trim()
        return trimmed == closing || trimmed.startsWith(closing)
    }

    private fun hasToken(element: PsiElement, tokenType: IElementType): Boolean {
        var node = element.node.firstChildNode
        while (node != null) {
            if (node.elementType == tokenType) return true
            val child = node.firstChildNode
            if (child != null && hasToken(node.psi, tokenType)) return true
            node = node.treeNext
        }
        return false
    }

    private companion object {
        private const val MAX_INLINE_LENGTH = 30
    }

    private fun isSimpleExpression(expression: ExExpression): Boolean = when (expression) {
        is ExAtom,
        is ExIdentifier,
        is ExAlias,
        is ExLiteral -> true

        else -> false
    }

    private fun onlyDoBlockExpression(element: PsiElement): ExExpression? {
        val doBlock = element as? ExDoBlock ?: return null
        val contents = PsiTreeUtil.getChildOfType(doBlock, ExDoContents::class.java) ?: return null
        if (PsiTreeUtil.findChildOfType(contents, ExDoBlock::class.java) != null) return null
        val expressions = contents.expressionList
        return expressions.singleOrNull()
    }

    private fun onlyFnBodyExpression(element: PsiElement): ExExpression? {
        val fnExpr = element as? ExFnExpr ?: return null
        val clauses = PsiTreeUtil.getChildrenOfTypeAsList(fnExpr, ExStabClause::class.java)
        val stabClause = clauses.singleOrNull() ?: return null
        var node = stabClause.node.firstChildNode
        var seenArrow = false
        var found: ExExpression? = null
        while (node != null) {
            if (node.elementType == EX_ARROW) {
                seenArrow = true
            } else if (seenArrow && node.psi is ExExpression) {
                if (found != null) return null
                found = node.psi as ExExpression
            }
            node = node.treeNext
        }
        return found
    }

    private data class InlineResult(
        val text: String?,
        val hasMore: Boolean,
        val endsWithComma: Boolean
    )
}
