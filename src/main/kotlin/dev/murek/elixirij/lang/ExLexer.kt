package dev.murek.elixirij.lang

import com.intellij.lexer.FlexAdapter

/**
 * Lexer adapter for the Elixir language.
 * Wraps the JFlex-generated _ExLexer.
 */
class ExLexer : FlexAdapter(_ExLexer())
