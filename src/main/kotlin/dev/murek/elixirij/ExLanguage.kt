package dev.murek.elixirij

import com.intellij.lang.Language

private const val LANGUAGE_ID = "Elixir"

/**
 * Elixir language definition.
 */
object ExLanguage : Language(LANGUAGE_ID) {
    override fun getDisplayName(): String = LANGUAGE_ID
}
