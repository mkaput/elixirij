package dev.murek.elixirij.lang

import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet
import dev.murek.elixirij.ExLanguage

private val FILE = IFileElementType(ExLanguage)

class ExParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = ExLexer()

    override fun createParser(project: Project?): PsiParser {
        // For now, we use a simple parser that doesn't do any parsing
        // This will be expanded later when we implement the full parser
        return PsiParser { root, builder ->
            val marker = builder.mark()
            while (!builder.eof()) {
                builder.advanceLexer()
            }
            marker.done(root)
            builder.treeBuilt
        }
    }

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getCommentTokens(): TokenSet = EX_COMMENTS

    override fun getStringLiteralElements(): TokenSet = EX_STRINGS

    override fun createElement(node: ASTNode): PsiElement {
        throw UnsupportedOperationException(node.elementType.toString())
    }

    override fun createFile(viewProvider: FileViewProvider): PsiFile = ExFile(viewProvider)
}
