package dev.murek.elixirij.highlighting

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.fileTypes.SyntaxHighlighter
import com.intellij.openapi.options.colors.AttributesDescriptor
import com.intellij.openapi.options.colors.ColorDescriptor
import com.intellij.openapi.options.colors.ColorSettingsPage
import dev.murek.elixirij.ExIcons
import dev.murek.elixirij.ExLanguage
import javax.swing.Icon

/**
 * Color settings page for Elixir language in IDE Settings.
 * Allows users to customize syntax highlighting colors for Elixir.
 */
class ExColorSettingsPage : ColorSettingsPage {

    override fun getDisplayName(): String = ExLanguage.displayName

    override fun getIcon(): Icon = ExIcons.FILE

    override fun getHighlighter(): SyntaxHighlighter = ExSyntaxHighlighter()

    override fun getColorDescriptors(): Array<ColorDescriptor> = ColorDescriptor.EMPTY_ARRAY

    override fun getDemoText(): String = DEMO_TEXT

    override fun getAttributeDescriptors(): Array<AttributesDescriptor> {
        return ExTextAttributes.entries.map { it.descriptor }.toTypedArray()
    }

    override fun getAdditionalHighlightingTagToDescriptorMap(): Map<String, TextAttributesKey>? = null

    companion object {
        private val DEMO_TEXT = """
            # This is a comment
            defmodule MyModule do
              @moduledoc "Module documentation"
              @version "1.0.0"
              
              def greet(name) do
                message = "Hello, #{name}!"
                IO.puts(message)
                :ok
              end
              
              defp private_function(x, y) do
                x + y * 2
              end
              
              def using_sigil do
                ~r/pattern/i
                ~s(string sigil)
              end
              
              def literals do
                integer = 42
                float = 3.14
                hex = 0xFF
                binary = 0b1010
                char = ?a
                atom = :my_atom
                quoted_atom = :"quoted atom"
                string = "hello world"
                charlist = 'charlist'
                list = [1, 2, 3]
                tuple = {1, 2, 3}
                map = %{key: "value"}
              end
              
              def operators do
                result = 1 + 2 - 3 * 4 / 5
                comparison = a == b and c != d
                pipe = data |> transform() |> output()
                range = 1..10
              end
            end
        """.trimIndent()
    }
}
