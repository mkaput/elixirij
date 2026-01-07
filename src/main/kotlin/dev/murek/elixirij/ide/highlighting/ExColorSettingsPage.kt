package dev.murek.elixirij.ide.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import dev.murek.elixirij.ExIcons
import dev.murek.elixirij.ExLanguage
import javax.swing.Icon

class ExColorSettingsPage : ColorSettingsPage {

    override fun getDisplayName(): String = ExLanguage.displayName

    override fun getIcon(): Icon = ExIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = ExSyntaxHighlighter()

    override fun getColorDescriptors(): Array<ColorDescriptor> = emptyArray()

    override fun getDemoText(): String =
        checkNotNull(javaClass.getResourceAsStream("/ColorSettingsPageDemo.exs")?.bufferedReader()?.readText())

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        return ExTextAttributes.entries.map { it.descriptor }.toTypedArray()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey> =
        mapOf(
            "SPECIAL_FORM" to ExTextAttributes.SPECIAL_FORM.attribute,
            "FUNCTION_CALL" to ExTextAttributes.FUNCTION_CALL.attribute,
            "FUNCTION_DECLARATION" to ExTextAttributes.FUNCTION_DECLARATION.attribute,
            "MACRO_DECLARATION" to ExTextAttributes.MACRO_DECLARATION.attribute,
            "MODULE_ATTRIBUTE" to ExTextAttributes.MODULE_ATTRIBUTE.attribute,
            "DOC_COMMENT" to ExTextAttributes.DOC_COMMENT.attribute,
            "UNUSED_VARIABLE" to ExTextAttributes.UNUSED_VARIABLE.attribute
        )
}
