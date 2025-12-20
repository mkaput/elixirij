package dev.murek.elixirij

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import dev.murek.elixirij.lsp.CodeIntelligenceService
import dev.murek.elixirij.lsp.ElixirLSMode
import dev.murek.elixirij.lsp.ExLspServerSupportProvider
import dev.murek.elixirij.lsp.ExLspSettings
import dev.murek.elixirij.lsp.ExpertMode

class ExConfigurable(private val project: Project) : BoundConfigurable(
    ExBundle.message("configurable.elixir.displayName")
) {
    private val lspSettings = ExLspSettings.getInstance(project)

    override fun createPanel(): DialogPanel = panel {
        row(ExBundle.message("configurable.codeIntelligenceService.label")) {
            comboBox(
                CodeIntelligenceService.entries, textListCellRenderer {
                    when (it) {
                        null -> null
                        CodeIntelligenceService.EXPERT -> ExBundle.message("configurable.codeIntelligenceService.expert")
                        CodeIntelligenceService.ELIXIR_LS -> ExBundle.message("configurable.codeIntelligenceService.elixirls")
                        CodeIntelligenceService.NONE -> ExBundle.message("configurable.codeIntelligenceService.none")
                    }
                }).bindItem(lspSettings::codeIntelligenceService.toMutableProperty().toNullableProperty())
        }

        group(ExBundle.message("configurable.expert.group.title")) {
            buttonsGroup {
                row {
                    radioButton(ExBundle.message("configurable.expert.mode.automatic"), ExpertMode.AUTOMATIC)
                }
                row {
                    radioButton(ExBundle.message("configurable.expert.mode.custom"), ExpertMode.CUSTOM)
                }
            }.bind(lspSettings::expertMode)

            row(ExBundle.message("configurable.expert.customPath.label")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
                        .withTitle(ExBundle.message("configurable.expert.customPath.browseTitle")), project
                ).bindText(lspSettings::expertCustomExecutablePath.toNonNullableProperty("")).align(AlignX.FILL)
            }
        }

        group(ExBundle.message("configurable.elixirls.group.title")) {
            buttonsGroup {
                row {
                    radioButton(ExBundle.message("configurable.elixirls.mode.automatic"), ElixirLSMode.AUTOMATIC)
                }
                row {
                    radioButton(ExBundle.message("configurable.elixirls.mode.custom"), ElixirLSMode.CUSTOM)
                }
            }.bind(lspSettings::elixirLSMode)

            row(ExBundle.message("configurable.elixirls.customPath.label")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
                        .withTitle(ExBundle.message("configurable.elixirls.customPath.browseTitle")), project
                ).bindText(lspSettings::elixirLSCustomExecutablePath.toNonNullableProperty("")).align(AlignX.FILL)
            }
        }
    }

    override fun apply() {
        super.apply()

        LspServerManager.getInstance(project).stopAndRestartIfNeeded(ExLspServerSupportProvider::class.java)
    }
}
