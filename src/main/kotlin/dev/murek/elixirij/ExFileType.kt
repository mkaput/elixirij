package dev.murek.elixirij

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * Elixir file type definition.
 */
object ExFileType : LanguageFileType(ExLanguage) {
    override fun getName(): String = "Elixir"
    
    override fun getDescription(): String = ExBundle.message("filetype.elixir.description")
    
    override fun getDefaultExtension(): String = "ex"
    
    override fun getIcon(): Icon = ExIcons.FILE
}
