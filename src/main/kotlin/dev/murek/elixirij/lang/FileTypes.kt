package dev.murek.elixirij.lang

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import dev.murek.elixirij.ExBundle
import dev.murek.elixirij.ExIcons
import dev.murek.elixirij.ExLanguage
import javax.swing.Icon

sealed class ExFileTypeBase(secondary: Boolean = false) : LanguageFileType(ExLanguage, secondary)

object ExFileType : ExFileTypeBase() {
    override fun getName(): String = "Elixir"

    override fun getDescription(): String = ExBundle.message("filetype.elixir.description")

    override fun getDefaultExtension(): String = "ex"

    override fun getIcon(): Icon = ExIcons.FILE
}

object ExsFileType : ExFileTypeBase(true) {

    override fun getName(): String = "Elixir Script"

    override fun getDisplayName(): String = ExBundle.message("filetype.elixirScript.name")

    override fun getDescription(): String = ExBundle.message("filetype.elixirScript.description")

    override fun getDefaultExtension(): String = "exs"

    override fun getIcon(): Icon = ExIcons.SCRIPT_FILE
}

val FileType.isElixir: Boolean get() = this is ExFileTypeBase
