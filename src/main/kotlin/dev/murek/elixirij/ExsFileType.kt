package dev.murek.elixirij

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * Elixir script file type definition.
 */
object ExsFileType : LanguageFileType(ExLanguage) {
    override fun getName(): String = "Elixir Script"

    override fun getDescription(): String = ExBundle.message("filetype.elixirScript.description")

    override fun getDefaultExtension(): String = "exs"

    override fun getIcon(): Icon = ExIcons.FILE
}
