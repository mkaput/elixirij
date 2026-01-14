package dev.murek.elixirij

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import dev.murek.elixirij.lsp.ExLspServerSupportProvider
import dev.murek.elixirij.toolchain.ExExecutableTestButton
import dev.murek.elixirij.toolchain.ExToolchain
import dev.murek.elixirij.toolchain.ExToolchainManager

class ExConfigurable(private val project: Project) : BoundConfigurable(
    ExBundle.message("configurable.elixir.displayName")
), SearchableConfigurable {
    private val settings = ExSettings.getInstance(project)
    private val toolchainProvider = ExToolchainManager.getInstance(project)

    override fun getId(): String = "dev.murek.elixirij.elixir"

    override fun createPanel(): DialogPanel = panel {
        group(ExBundle.message("configurable.toolchain.group.title")) {
            lateinit var toolchainField: TextFieldWithBrowseButton
            lateinit var testButton: ExExecutableTestButton
            row(ExBundle.message("configurable.toolchain.path.label")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
                        .withTitle(ExBundle.message("configurable.toolchain.path.browseTitle")), project
                )
                    .applyToComponent {
                        toolchainField = this

                        // Show auto-detected path as placeholder
                        (textField as? JBTextField)?.emptyText?.text =
                            toolchainProvider.detectedToolchain?.elixir?.toString() ?: ""
                    }
                    .bindText(settings::elixirToolchainPath.toNonNullableProperty(""))
                    .align(AlignX.FILL)
                    .textValidation(ExToolchain.CHECK_VALID)
                    .onChanged {
                        testButton.updateTestButton(it.text)
                    }
            }
            row {
                cell(
                    ExExecutableTestButton(
                        project,
                        toolchainField,
                        toolchainProvider,
                        settings.coroutineScope,
                        disposable
                    )
                )
                    .applyToComponent {
                        testButton = this
                    }
            }
        }

        group(ExBundle.message("configurable.expert.group.title")) {
            buttonsGroup {
                row {
                    radioButton(ExBundle.message("configurable.expert.mode.disabled"), ExpertMode.DISABLED)
                }
                row {
                    radioButton(ExBundle.message("configurable.expert.mode.automatic"), ExpertMode.AUTOMATIC)
                }
                row {
                    radioButton(ExBundle.message("configurable.expert.mode.custom"), ExpertMode.CUSTOM)
                }
            }.bind(settings::expertMode)

            row(ExBundle.message("configurable.expert.customPath.label")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
                        .withTitle(ExBundle.message("configurable.expert.customPath.browseTitle")), project
                )
                    .bindText(settings::expertCustomExecutablePath.toNonNullableProperty(""))
                    .align(AlignX.FILL)
            }
        }
    }

    override fun apply() {
        super.apply()

        LspServerManager.getInstance(project).stopAndRestartIfNeeded(ExLspServerSupportProvider::class.java)
    }
}
