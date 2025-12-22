package dev.murek.elixirij.ide.highlighting

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import dev.murek.elixirij.lang.EX_ALIAS
import dev.murek.elixirij.lang.EX_AT
import dev.murek.elixirij.lang.EX_ATOM
import dev.murek.elixirij.lang.EX_ATOM_QUOTED
import dev.murek.elixirij.lang.EX_BINARY_DELIMITERS
import dev.murek.elixirij.lang.EX_BRACES
import dev.murek.elixirij.lang.EX_BRACKETS
import dev.murek.elixirij.lang.EX_COMMA
import dev.murek.elixirij.lang.EX_COMMENTS
import dev.murek.elixirij.lang.EX_DOT
import dev.murek.elixirij.lang.EX_IDENTIFIER
import dev.murek.elixirij.lang.EX_KEYWORDS
import dev.murek.elixirij.lang.EX_KW_IDENTIFIER
import dev.murek.elixirij.lang.EX_NUMBERS
import dev.murek.elixirij.lang.EX_OPERATORS
import dev.murek.elixirij.lang.EX_PARENS
import dev.murek.elixirij.lang.EX_SEMICOLON
import dev.murek.elixirij.lang.EX_SIGIL
import dev.murek.elixirij.lang.EX_STRINGS
import dev.murek.elixirij.lang.lexer.ExLexer

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
        EX_ATOM, EX_ATOM_QUOTED, EX_KW_IDENTIFIER -> pack(ExTextAttributes.ATOM)
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
