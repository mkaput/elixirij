package dev.murek.elixirij.ide.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.OptionsBundle
import com.intellij.openapi.options.colors.AttributesDescriptor
import dev.murek.elixirij.ExBundle
import java.util.function.Supplier

/**
 * Defines text attributes for Elixir syntax highlighting.
 * Each attribute maps to a specific token type and provides a fallback to
 * standard IntelliJ colors for consistent appearance across themes.
 */
enum class ExTextAttributes(
    externalName: String,
    fallbackKey: TextAttributesKey?,
    displayNameSupplier: Supplier<String>
) {
    COMMENT(
        "ELIXIR_COMMENT",
        DefaultLanguageHighlighterColors.LINE_COMMENT,
        OptionsBundle.messagePointer("options.language.defaults.line.comment")
    ),
    KEYWORD(
        "ELIXIR_KEYWORD",
        DefaultLanguageHighlighterColors.KEYWORD,
        OptionsBundle.messagePointer("options.language.defaults.keyword")
    ),
    STRING(
        "ELIXIR_STRING",
        DefaultLanguageHighlighterColors.STRING,
        OptionsBundle.messagePointer("options.language.defaults.string")
    ),
    NUMBER(
        "ELIXIR_NUMBER",
        DefaultLanguageHighlighterColors.NUMBER,
        OptionsBundle.messagePointer("options.language.defaults.number")
    ),
    ATOM(
        "ELIXIR_ATOM",
        DefaultLanguageHighlighterColors.CONSTANT,
        ExBundle.messagePointer("color.settings.atom")
    ),
    MODULE(
        "ELIXIR_MODULE",
        DefaultLanguageHighlighterColors.CLASS_NAME,
        ExBundle.messagePointer("color.settings.module")
    ),
    IDENTIFIER(
        "ELIXIR_IDENTIFIER",
        DefaultLanguageHighlighterColors.IDENTIFIER,
        OptionsBundle.messagePointer("options.language.defaults.identifier")
    ),
    OPERATOR(
        "ELIXIR_OPERATOR",
        DefaultLanguageHighlighterColors.OPERATION_SIGN,
        OptionsBundle.messagePointer("options.language.defaults.operation")
    ),
    BRACES(
        "ELIXIR_BRACES",
        DefaultLanguageHighlighterColors.BRACES,
        OptionsBundle.messagePointer("options.language.defaults.braces")
    ),
    BRACKETS(
        "ELIXIR_BRACKETS",
        DefaultLanguageHighlighterColors.BRACKETS,
        OptionsBundle.messagePointer("options.language.defaults.brackets")
    ),
    BINARY_DELIMITERS(
        "ELIXIR_BINARY_DELIMITERS",
        DefaultLanguageHighlighterColors.BRACES,
        ExBundle.messagePointer("color.settings.binaryDelimiters")
    ),
    PARENTHESES(
        "ELIXIR_PARENTHESES",
        DefaultLanguageHighlighterColors.PARENTHESES,
        OptionsBundle.messagePointer("options.language.defaults.parentheses")
    ),
    COMMA(
        "ELIXIR_COMMA",
        DefaultLanguageHighlighterColors.COMMA,
        OptionsBundle.messagePointer("options.language.defaults.comma")
    ),
    SEMICOLON(
        "ELIXIR_SEMICOLON",
        DefaultLanguageHighlighterColors.SEMICOLON,
        OptionsBundle.messagePointer("options.language.defaults.semicolon")
    ),
    DOT(
        "ELIXIR_DOT",
        DefaultLanguageHighlighterColors.DOT,
        OptionsBundle.messagePointer("options.language.defaults.dot")
    ),
    SIGIL(
        "ELIXIR_SIGIL",
        DefaultLanguageHighlighterColors.STRING,
        ExBundle.messagePointer("color.settings.sigil")
    ),
    MODULE_ATTRIBUTE(
        "ELIXIR_MODULE_ATTRIBUTE",
        DefaultLanguageHighlighterColors.METADATA,
        ExBundle.messagePointer("color.settings.attribute")
    ),
    BAD_CHARACTER(
        "ELIXIR_BAD_CHARACTER",
        HighlighterColors.BAD_CHARACTER,
        OptionsBundle.messagePointer("options.java.attribute.descriptor.bad.character")
    );

    val attribute: TextAttributesKey = TextAttributesKey.createTextAttributesKey(externalName, fallbackKey)
    val descriptor: AttributesDescriptor = AttributesDescriptor(displayNameSupplier, attribute)
}
