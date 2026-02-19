package dev.murek.elixirij.ide.formatter

import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import dev.murek.elixirij.ExLanguage

class ExLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage() = ExLanguage

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions
    ) {
        indentOptions.INDENT_SIZE = 2
        indentOptions.TAB_SIZE = 2
        indentOptions.CONTINUATION_INDENT_SIZE = 2
        indentOptions.USE_TAB_CHARACTER = false
    }

    override fun getCodeSample(settingsType: SettingsType): String =
        checkNotNull(javaClass.getResourceAsStream("/CodeStyleSettingsPageDemo.exs")).bufferedReader().readText()
}
