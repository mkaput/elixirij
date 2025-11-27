package dev.murek.elixirij.psi

import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import dev.murek.elixirij.ExBundle
import dev.murek.elixirij.ExFileType
import dev.murek.elixirij.ExLanguage

/**
 * PSI file representation for Elixir files.
 */
class ExFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, ExLanguage) {
    override fun getFileType(): FileType = ExFileType

    override fun toString(): String = ExBundle.message("file.elixir.name")
}
