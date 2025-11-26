package dev.murek.elixirij

import com.intellij.lang.Language

/**
 * Elixir language definition.
 */
object ExLanguage : Language("Elixir") {
    const val ID = "Elixir"
    
    override fun getDisplayName(): String = "Elixir"
}
