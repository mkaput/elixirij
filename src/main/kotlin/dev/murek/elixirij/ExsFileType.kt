package dev.murek.elixirij

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object ExsFileType : LanguageFileType(ExLanguage, true) {
    override fun getName(): String = "Elixir Script"

    override fun getDisplayName(): String = ExBundle.message("filetype.elixirScript.name")

    override fun getDescription(): String = ExBundle.message("filetype.elixirScript.description")

    override fun getDefaultExtension(): String = "exs"

    override fun getIcon(): Icon = ExIcons.SCRIPT_FILE
}
