package dev.murek.elixirij.lang.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiErrorElement

class ExErrorElement(node: ASTNode) : ASTWrapperPsiElement(node), PsiErrorElement {
    override fun getErrorDescription(): String = text
}
