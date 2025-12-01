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

class ExSyntaxHighlighter : SyntaxHighlighterBase() {

    class Factory : SyntaxHighlighterFactory() {
        override fun getSyntaxHighlighter(project: Project?, virtualFile: VirtualFile?): SyntaxHighlighter =
            ExSyntaxHighlighter()
    }

    override fun getHighlightingLexer(): Lexer = ExLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when (tokenType) {
        in EX_COMMENTS -> pack(ExTextAttributes.COMMENT)
        in EX_KEYWORDS -> pack(ExTextAttributes.KEYWORD)
        in EX_STRINGS -> pack(ExTextAttributes.STRING)
        in EX_NUMBERS -> pack(ExTextAttributes.NUMBER)
        EX_ATOM, EX_ATOM_QUOTED -> pack(ExTextAttributes.ATOM)
        EX_ALIAS -> pack(ExTextAttributes.MODULE)
        EX_IDENTIFIER -> pack(ExTextAttributes.IDENTIFIER)
        EX_SIGIL -> pack(ExTextAttributes.SIGIL)
        // Handle EX_AT before EX_OPERATORS since @ is used for module attributes in Elixir
        EX_AT -> pack(ExTextAttributes.MODULE_ATTRIBUTE)
        in EX_OPERATORS -> pack(ExTextAttributes.OPERATOR)
        in EX_BRACES -> pack(ExTextAttributes.BRACES)
        in EX_BRACKETS -> pack(ExTextAttributes.BRACKETS)
        in EX_BINARY_DELIMITERS -> pack(ExTextAttributes.BINARY_DELIMITERS)
        in EX_PARENS -> pack(ExTextAttributes.PARENTHESES)
        EX_COMMA -> pack(ExTextAttributes.COMMA)
        EX_SEMICOLON -> pack(ExTextAttributes.SEMICOLON)
        EX_DOT -> pack(ExTextAttributes.DOT)
        TokenType.BAD_CHARACTER -> pack(ExTextAttributes.BAD_CHARACTER)
        else -> emptyArray()
    }

    private fun pack(attr: ExTextAttributes): Array<TextAttributesKey> = arrayOf(attr.attribute)
}
