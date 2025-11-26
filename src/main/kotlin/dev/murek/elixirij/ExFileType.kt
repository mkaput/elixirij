package dev.murek.elixirij

import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * Elixir file type definition.
 */
object ExFileType : LanguageFileType(ExLanguage) {
    const val DEFAULT_EXTENSION = "ex"
    
    override fun getName(): String = "Elixir"
    
    override fun getDescription(): String = ExBundle.message("filetype.elixir.description")
    
    override fun getDefaultExtension(): String = DEFAULT_EXTENSION
    
    override fun getIcon(): Icon = ExIcons.FILE
}
