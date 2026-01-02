package dev.murek.elixirij.lang.spellchecker

import com.intellij.spellchecker.BundledDictionaryProvider

/**
 * Provides bundled dictionary for Elixir language with common Elixir, Erlang, and framework terms.
 */
class ExBundledDictionaryProvider : BundledDictionaryProvider {
    override fun getBundledDictionaries(): Array<String> = arrayOf("/dictionaries/elixir.dic")
}
