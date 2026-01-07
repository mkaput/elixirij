package dev.murek.elixirij.ide.highlighting

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.options.OptionsBundle
import com.intellij.openapi.options.colors.AttributesDescriptor
import dev.murek.elixirij.ExBundle
import java.util.function.Supplier

/**
 * Defines text attributes for Elixir syntax highlighting.
 * Each attribute maps to a specific token type and provides a fallback to
 * standard IntelliJ colors for consistent appearance across themes.
 *
 * When adding a new attribute here, update [ExColorSettingsPage] and
 * `ColorSettingsPageDemo.exs` to keep the demo tags in sync.
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
    DOC_COMMENT(
        "ELIXIR_DOC_COMMENT",
        DefaultLanguageHighlighterColors.DOC_COMMENT,
        OptionsBundle.messagePointer("options.language.defaults.doc.comment")
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
    STRING_ESCAPE(
        "ELIXIR_STRING_ESCAPE",
        DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE,
        ExBundle.messagePointer("color.settings.stringEscape")
    ),
    INVALID_STRING_ESCAPE(
        "ELIXIR_INVALID_STRING_ESCAPE",
        DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE,
        ExBundle.messagePointer("color.settings.invalidStringEscape")
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
    FUNCTION_DECLARATION(
        "ELIXIR_FUNCTION_DECLARATION",
        DefaultLanguageHighlighterColors.FUNCTION_DECLARATION,
        ExBundle.messagePointer("color.settings.functionDeclaration")
    ),
    MACRO_DECLARATION(
        "ELIXIR_MACRO_DECLARATION",
        DefaultLanguageHighlighterColors.FUNCTION_DECLARATION,
        ExBundle.messagePointer("color.settings.macroDeclaration")
    ),
    SPECIAL_FORM(
        "ELIXIR_SPECIAL_FORM",
        DefaultLanguageHighlighterColors.KEYWORD,
        ExBundle.messagePointer("color.settings.specialForm")
    ),
    UNUSED_VARIABLE(
        "ELIXIR_UNUSED_VARIABLE",
        CodeInsightColors.NOT_USED_ELEMENT_ATTRIBUTES,
        ExBundle.messagePointer("color.settings.unusedVariable")
    ),
    BAD_CHARACTER(
        "ELIXIR_BAD_CHARACTER",
        HighlighterColors.BAD_CHARACTER,
        OptionsBundle.messagePointer("options.java.attribute.descriptor.bad.character")
    );

    val attribute: TextAttributesKey = TextAttributesKey.createTextAttributesKey(externalName, fallbackKey)
    val descriptor: AttributesDescriptor = AttributesDescriptor(displayNameSupplier, attribute)
}
