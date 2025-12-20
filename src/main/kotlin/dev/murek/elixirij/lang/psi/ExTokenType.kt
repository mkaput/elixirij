package dev.murek.elixirij.lang.psi

import com.intellij.psi.tree.IElementType
import dev.murek.elixirij.ExLanguage

class ExTokenType(debugName: String) : IElementType(debugName, ExLanguage) {
    override fun toString(): String = "ExTokenType.${super.toString()}"
}
