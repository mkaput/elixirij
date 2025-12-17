package dev.murek.elixirij

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.actionListener
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import dev.murek.elixirij.lsp.ExLspSettings
import dev.murek.elixirij.lsp.ExLspServerSupportProvider
import dev.murek.elixirij.lsp.ExpertMode

class ExConfigurable(private val project: Project) : BoundConfigurable(
    ExBundle.message("configurable.elixir.displayName")
) {
    private val lspSettings = ExLspSettings.getInstance(project)

    // Observable property for mode - drives visibility of the custom path row
    private val modeProperty = AtomicProperty(lspSettings.expertMode)

    override fun createPanel(): DialogPanel = panel {
        group(ExBundle.message("configurable.expert.group.title")) {
            buttonsGroup {
                row {
                    radioButton(ExBundle.message("configurable.expert.mode.disabled"), ExpertMode.DISABLED)
                        .actionListener { _, _ -> modeProperty.set(ExpertMode.DISABLED) }
                }
                row {
                    radioButton(ExBundle.message("configurable.expert.mode.automatic"), ExpertMode.AUTOMATIC)
                        .actionListener { _, _ -> modeProperty.set(ExpertMode.AUTOMATIC) }
                }
                row {
                    radioButton(ExBundle.message("configurable.expert.mode.custom"), ExpertMode.CUSTOM)
                        .actionListener { _, _ -> modeProperty.set(ExpertMode.CUSTOM) }
                }
            }.bind(lspSettings::expertMode)

            row(ExBundle.message("configurable.expert.customPath.label")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
                        .withTitle(ExBundle.message("configurable.expert.customPath.browseTitle")),
                    project
                )
                    .bindText(
                        getter = { lspSettings.expertCustomExecutablePath ?: "" },
                        setter = { lspSettings.expertCustomExecutablePath = it.ifEmpty { null } }
                    )
                    .align(AlignX.FILL)
            }.visibleIf(modeProperty.transform { it == ExpertMode.CUSTOM })
        }
    }

    override fun apply() {
        super.apply()

        LspServerManager.getInstance(project).stopAndRestartIfNeeded(ExLspServerSupportProvider::class.java)
    }

    override fun reset() {
        super.reset()
        modeProperty.set(lspSettings.expertMode)
    }
}
