package dev.murek.elixirij.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import dev.murek.elixirij.lexer.ExLexer
import dev.murek.elixirij.psi.*

/**
 * Syntax highlighter for Elixir language.
 * Maps token types to text attributes for syntax highlighting.
 */
class ExSyntaxHighlighter : SyntaxHighlighterBase() {

    /**
     * Factory for creating Elixir syntax highlighters.
     */
    class Factory : SyntaxHighlighterFactory() {
        override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter {
            return ExSyntaxHighlighter()
        }
    }

    override fun getHighlightingLexer(): Lexer = ExLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> {
        return when {
            tokenType in EX_COMMENTS -> pack(ExTextAttributes.COMMENT)
            tokenType in EX_KEYWORDS -> pack(ExTextAttributes.KEYWORD)
            tokenType in EX_STRINGS -> pack(ExTextAttributes.STRING)
            tokenType in EX_NUMBERS -> pack(ExTextAttributes.NUMBER)
            tokenType == EX_ATOM || tokenType == EX_ATOM_QUOTED -> pack(ExTextAttributes.ATOM)
            tokenType == EX_ALIAS -> pack(ExTextAttributes.MODULE)
            tokenType == EX_IDENTIFIER -> pack(ExTextAttributes.IDENTIFIER)
            tokenType in EX_OPERATORS -> pack(ExTextAttributes.OPERATOR)
            tokenType == EX_SIGIL -> pack(ExTextAttributes.SIGIL)
            tokenType == EX_AT -> pack(ExTextAttributes.MODULE_ATTRIBUTE)
            tokenType == EX_LBRACE || tokenType == EX_RBRACE || tokenType == EX_PERCENT_LBRACE -> pack(ExTextAttributes.BRACES)
            tokenType == EX_LBRACKET || tokenType == EX_RBRACKET || tokenType == EX_LT_LT || tokenType == EX_GT_GT -> pack(ExTextAttributes.BRACKETS)
            tokenType == EX_LPAREN || tokenType == EX_RPAREN -> pack(ExTextAttributes.PARENTHESES)
            tokenType == EX_COMMA -> pack(ExTextAttributes.COMMA)
            tokenType == EX_SEMICOLON -> pack(ExTextAttributes.SEMICOLON)
            tokenType == EX_DOT -> pack(ExTextAttributes.DOT)
            tokenType == TokenType.BAD_CHARACTER -> pack(ExTextAttributes.BAD_CHARACTER)
            else -> EMPTY_KEYS
        }
    }

    private fun pack(attr: ExTextAttributes): Array<TextAttributesKey> {
        return arrayOf(attr.attribute)
    }

    companion object {
        private val EMPTY_KEYS = emptyArray<TextAttributesKey>()
    }
}
