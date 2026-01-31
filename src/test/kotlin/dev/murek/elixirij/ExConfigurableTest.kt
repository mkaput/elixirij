package dev.murek.elixirij

import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.testFramework.LightPlatformTestCase
import java.awt.Component
import java.awt.Container
import javax.swing.JRadioButton

class ExConfigurableTest : LightPlatformTestCase() {

    private val settings get() = ExSettings.getInstance(project)

    override fun tearDown() {
        try {
            settings.reset()
        } finally {
            super.tearDown()
        }
    }

    fun `test toolchain path updates setting`() {
        settings.elixirToolchainPath = "/tmp/elixir"

        configure { panel ->
            val field = panel.find<TextFieldWithBrowseButton>("toolchainPathField")
            assertEquals("/tmp/elixir", field.text)
            field.text = "/tmp/new-elixir"
        }

        assertEquals("/tmp/new-elixir", settings.elixirToolchainPath)
    }

    fun `test expert mode updates setting`() {
        settings.expertMode = ExpertMode.AUTOMATIC

        configure { panel ->
            val btn = panel.find<JRadioButton>("expertModeCustomRadio")
            btn.isSelected = true
        }

        assertEquals(ExpertMode.CUSTOM, settings.expertMode)
    }

    fun `test expert custom path updates setting`() {
        settings.expertCustomExecutablePath = "/tmp/expert"

        configure { panel ->
            val field = panel.find<TextFieldWithBrowseButton>("expertCustomPathField")
            assertEquals("/tmp/expert", field.text)
            field.text = "/tmp/new-expert"
        }

        assertEquals("/tmp/new-expert", settings.expertCustomExecutablePath)
    }

    private fun configure(f: (DialogPanel) -> Unit) {
        val configurable = ExConfigurable(project)
        try {
            val panel = configurable.createPanel()
            f(panel)
            panel.apply()
        } finally {
            configurable.disposeUIResources()
        }
    }

    private inline fun <reified T : Component> Container.find(name: String): T = findBy<T> { it.name == name }

    private inline fun <reified T> Container.findBy(
        noinline condition: (T) -> Boolean = { true },
    ): T = collectComponents(this, T::class.java, condition).single()

    private fun <T> collectComponents(
        container: Container,
        clazz: Class<T>,
        condition: (T) -> Boolean,
    ): List<T> {
        val results = mutableListOf<T>()
        for (component in container.components) {
            if (clazz.isInstance(component)) {
                val cast = clazz.cast(component)
                if (condition(cast)) {
                    results.add(cast)
                }
            }
            if (component is Container) {
                results.addAll(collectComponents(component, clazz, condition))
            }
        }
        return results
    }
}
