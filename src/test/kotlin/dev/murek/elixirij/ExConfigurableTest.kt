package dev.murek.elixirij

import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.registerOrReplaceServiceInstance
import dev.murek.elixirij.lsp.Expert
import java.awt.Component
import java.awt.Container
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
        settings.expertMode = ExpertMode.CUSTOM
        settings.expertCustomExecutablePath = "/tmp/expert"

        configure { panel ->
            val field = panel.find<TextFieldWithBrowseButton>("expertCustomPathField")
            assertTrue(field.isEnabled)
            assertEquals("/tmp/expert", field.text)
            field.text = "/tmp/new-expert"
        }

        assertEquals("/tmp/new-expert", settings.expertCustomExecutablePath)
    }

    fun `test expert release channel updates setting`() {
        settings.expertMode = ExpertMode.AUTOMATIC
        settings.expertReleaseChannel = ExpertReleaseChannel.STABLE

        configure { panel ->
            val btn = panel.find<JRadioButton>("expertReleaseChannelNightlyRadio")
            assertTrue(btn.isEnabled)
            btn.isSelected = true
        }

        assertEquals(ExpertReleaseChannel.NIGHTLY, settings.expertReleaseChannel)
    }

    fun `test expert release channel is disabled when mode is not automatic`() {
        settings.expertMode = ExpertMode.CUSTOM
        settings.expertReleaseChannel = ExpertReleaseChannel.NIGHTLY

        configure { panel ->
            val stableBtn = panel.find<JRadioButton>("expertReleaseChannelStableRadio")
            val nightlyBtn = panel.find<JRadioButton>("expertReleaseChannelNightlyRadio")
            assertFalse(stableBtn.isEnabled)
            assertFalse(nightlyBtn.isEnabled)
        }

        assertEquals(ExpertReleaseChannel.NIGHTLY, settings.expertReleaseChannel)
    }

    fun `test expert custom path is disabled when mode is not custom`() {
        settings.expertMode = ExpertMode.AUTOMATIC
        settings.expertCustomExecutablePath = "/tmp/expert"

        configure { panel ->
            val field = panel.find<TextFieldWithBrowseButton>("expertCustomPathField")
            assertFalse(field.isEnabled)
        }

        assertEquals("/tmp/expert", settings.expertCustomExecutablePath)
    }

    fun `test applying settings in automatic mode triggers updates check`() {
        settings.expertMode = ExpertMode.AUTOMATIC
        settings.expertReleaseChannel = ExpertReleaseChannel.STABLE

        val job = Job()
        val replacementExpert = Expert(project, CoroutineScope(job + NonExecutingDispatcher))
        project.registerOrReplaceServiceInstance(Expert::class.java, replacementExpert, testRootDisposable)

        try {
            configureAndApply { panel ->
                val nightlyBtn = panel.find<JRadioButton>("expertReleaseChannelNightlyRadio")
                nightlyBtn.doClick()
            }

            assertTrue(replacementExpert.isDownloading)
        } finally {
            job.cancel()
        }
    }

    private fun configure(configurable: ExConfigurable = ExConfigurable(project), f: (DialogPanel) -> Unit) {
        try {
            val panel = configurable.createPanel()
            f(panel)
            panel.apply()
        } finally {
            configurable.disposeUIResources()
        }
    }

    private fun configureAndApply(f: (DialogPanel) -> Unit) {
        val configurable = ExConfigurable(project)
        try {
            val panel = configurable.createPanel()
            f(panel)
            configurable.apply()
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

    private object NonExecutingDispatcher : CoroutineDispatcher() {
        override fun dispatch(context: kotlin.coroutines.CoroutineContext, block: Runnable) {
            // Intentionally no-op to keep launched download coroutine suspended in tests.
        }
    }
}
