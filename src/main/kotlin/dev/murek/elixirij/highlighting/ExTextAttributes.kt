package dev.murek.elixirij.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.colors.AttributesDescriptor
import dev.murek.elixirij.ExBundle

/**
 * Defines text attributes for Elixir syntax highlighting.
 * Each attribute maps to a specific token type and provides a fallback to
 * standard IntelliJ colors for consistent appearance across themes.
 */
enum class ExTextAttributes(
    externalName: String,
    fallbackKey: TextAttributesKey?,
    displayName: String
) {
    COMMENT(
        "ELIXIR_COMMENT",
        DefaultLanguageHighlighterColors.LINE_COMMENT,
        "color.settings.comment"
    ),
    KEYWORD(
        "ELIXIR_KEYWORD",
        DefaultLanguageHighlighterColors.KEYWORD,
        "color.settings.keyword"
    ),
    STRING(
        "ELIXIR_STRING",
        DefaultLanguageHighlighterColors.STRING,
        "color.settings.string"
    ),
    NUMBER(
        "ELIXIR_NUMBER",
        DefaultLanguageHighlighterColors.NUMBER,
        "color.settings.number"
    ),
    ATOM(
        "ELIXIR_ATOM",
        DefaultLanguageHighlighterColors.CONSTANT,
        "color.settings.atom"
    ),
    MODULE(
        "ELIXIR_MODULE",
        DefaultLanguageHighlighterColors.CLASS_NAME,
        "color.settings.module"
    ),
    IDENTIFIER(
        "ELIXIR_IDENTIFIER",
        DefaultLanguageHighlighterColors.IDENTIFIER,
        "color.settings.identifier"
    ),
    OPERATOR(
        "ELIXIR_OPERATOR",
        DefaultLanguageHighlighterColors.OPERATION_SIGN,
        "color.settings.operator"
    ),
    BRACES(
        "ELIXIR_BRACES",
        DefaultLanguageHighlighterColors.BRACES,
        "color.settings.braces"
    ),
    BRACKETS(
        "ELIXIR_BRACKETS",
        DefaultLanguageHighlighterColors.BRACKETS,
        "color.settings.brackets"
    ),
    BINARY_DELIMITERS(
        "ELIXIR_BINARY_DELIMITERS",
        DefaultLanguageHighlighterColors.BRACES,
        "color.settings.binary.delimiters"
    ),
    PARENTHESES(
        "ELIXIR_PARENTHESES",
        DefaultLanguageHighlighterColors.PARENTHESES,
        "color.settings.parentheses"
    ),
    COMMA(
        "ELIXIR_COMMA",
        DefaultLanguageHighlighterColors.COMMA,
        "color.settings.comma"
    ),
    SEMICOLON(
        "ELIXIR_SEMICOLON",
        DefaultLanguageHighlighterColors.SEMICOLON,
        "color.settings.semicolon"
    ),
    DOT(
        "ELIXIR_DOT",
        DefaultLanguageHighlighterColors.DOT,
        "color.settings.dot"
    ),
    SIGIL(
        "ELIXIR_SIGIL",
        DefaultLanguageHighlighterColors.STRING,
        "color.settings.sigil"
    ),
    MODULE_ATTRIBUTE(
        "ELIXIR_MODULE_ATTRIBUTE",
        DefaultLanguageHighlighterColors.METADATA,
        "color.settings.module.attribute"
    ),
    BAD_CHARACTER(
        "ELIXIR_BAD_CHARACTER",
        HighlighterColors.BAD_CHARACTER,
        "color.settings.bad.character"
    );

    val attribute: TextAttributesKey = TextAttributesKey.createTextAttributesKey(externalName, fallbackKey)
    val descriptor: AttributesDescriptor = AttributesDescriptor(ExBundle.message(displayName), attribute)
}
