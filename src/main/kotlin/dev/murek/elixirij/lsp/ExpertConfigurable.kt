package dev.murek.elixirij.lsp

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.util.transform
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import dev.murek.elixirij.ExBundle

class ExpertConfigurable(private val project: Project) : BoundConfigurable(
    ExBundle.message("configurable.elixir.displayName")
) {
    private val settings = ExpertSettings.getInstance(project)

    // Observable property for mode - drives visibility of the custom path row
    private val modeProperty = AtomicProperty(settings.mode)

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
            }.bind(settings::mode)

            row(ExBundle.message("configurable.expert.customPath.label")) {
                textFieldWithBrowseButton(
                    FileChooserDescriptorFactory.createSingleFileOrExecutableAppDescriptor()
                        .withTitle(ExBundle.message("configurable.expert.customPath.browseTitle")),
                    project
                )
                    .bindText(
                        getter = { settings.customExecutablePath ?: "" },
                        setter = { settings.customExecutablePath = it.ifEmpty { null } }
                    )
                    .align(AlignX.FILL)
            }.visibleIf(modeProperty.transform { it == ExpertMode.CUSTOM })
        }
    }

    override fun apply() {
        super.apply()
        settings.fireSettingsChanged()
    }

    override fun reset() {
        super.reset()
        modeProperty.set(settings.mode)
    }
}
