package dev.murek.elixirij.lsp

import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBRadioButton
import java.awt.Container
import javax.swing.JComponent

class ExpertConfigurableTest : BasePlatformTestCase() {

    private lateinit var settings: ExpertSettings
    private lateinit var configurable: ExpertConfigurable

    override fun setUp() {
        super.setUp()
        settings = ExpertSettings.getInstance(project)
        // Reset settings to defaults
        settings.mode = ExpertMode.AUTOMATIC
        settings.customExecutablePath = null
    }

    override fun tearDown() {
        try {
            if (::configurable.isInitialized) {
                configurable.disposeUIResources()
            }
        } finally {
            super.tearDown()
        }
    }

    private fun createConfigurable(): ExpertConfigurable {
        configurable = ExpertConfigurable(project)
        configurable.createComponent()
        configurable.reset()
        return configurable
    }

    fun `test creates panel successfully`() {
        val configurable = createConfigurable()
        assertNotNull(configurable.createComponent())
    }

    fun `test panel contains three radio buttons`() {
        val configurable = createConfigurable()
        val panel = configurable.createComponent()
        val radioButtons = findAllComponents<JBRadioButton>(panel)
        assertEquals("Should have 3 radio buttons", 3, radioButtons.size)
    }

    fun `test panel contains text field with browse button`() {
        val configurable = createConfigurable()
        val panel = configurable.createComponent()
        val textFields = findAllComponents<TextFieldWithBrowseButton>(panel)
        assertEquals("Should have 1 text field with browse button", 1, textFields.size)
    }

    fun `test isModified returns false initially`() {
        val configurable = createConfigurable()
        assertFalse("Should not be modified initially", configurable.isModified)
    }

    fun `test isModified returns true after selecting different mode`() {
        val configurable = createConfigurable()
        val panel = configurable.createComponent()

        // Find and select the DISABLED radio button
        val radioButtons = findAllComponents<JBRadioButton>(panel)
        val disabledButton = radioButtons.find { it.text.contains("Disable") }
        assertNotNull("Should find Disabled radio button", disabledButton)

        disabledButton!!.doClick()

        assertTrue("Should be modified after changing mode", configurable.isModified)
    }

    fun `test apply saves mode change to settings`() {
        val configurable = createConfigurable()
        val panel = configurable.createComponent()

        // Select DISABLED mode
        val radioButtons = findAllComponents<JBRadioButton>(panel)
        val disabledButton = radioButtons.find { it.text.contains("Disable") }!!
        disabledButton.doClick()

        configurable.apply()

        assertEquals("Settings mode should be DISABLED", ExpertMode.DISABLED, settings.mode)
    }

    fun `test apply saves custom path to settings`() {
        // Start with CUSTOM mode so the text field is active
        settings.mode = ExpertMode.CUSTOM
        val configurable = createConfigurable()
        val panel = configurable.createComponent()

        val textField = findAllComponents<TextFieldWithBrowseButton>(panel).first()
        textField.text = "/custom/path/to/expert"

        configurable.apply()

        assertEquals("Settings should have custom path", "/custom/path/to/expert", settings.customExecutablePath)
    }

    fun `test apply saves empty custom path as null`() {
        settings.mode = ExpertMode.CUSTOM
        settings.customExecutablePath = "/some/path"
        val configurable = createConfigurable()
        val panel = configurable.createComponent()

        val textField = findAllComponents<TextFieldWithBrowseButton>(panel).first()
        textField.text = ""

        configurable.apply()

        assertNull("Empty path should be saved as null", settings.customExecutablePath)
    }

    fun `test reset restores settings values`() {
        settings.mode = ExpertMode.CUSTOM
        settings.customExecutablePath = "/some/path"

        val configurable = createConfigurable()

        // Modify the UI
        val panel = configurable.createComponent()
        val radioButtons = findAllComponents<JBRadioButton>(panel)
        val disabledButton = radioButtons.find { it.text.contains("Disable") }!!
        disabledButton.doClick()

        assertTrue("Should be modified", configurable.isModified)

        // Reset should restore original values
        configurable.reset()

        assertFalse("Should not be modified after reset", configurable.isModified)
    }

    fun `test displayName is Elixir`() {
        val configurable = createConfigurable()
        assertEquals("Elixir", configurable.displayName)
    }

    fun `test custom path field visibility toggles with mode`() {
        val configurable = createConfigurable()
        val panel = configurable.createComponent()
        val textField = findAllComponents<TextFieldWithBrowseButton>(panel).first()
        val radioButtons = findAllComponents<JBRadioButton>(panel)

        // Initially AUTOMATIC mode - find the parent row and check visibility
        val customButton = radioButtons.find { it.text.contains("Custom") }!!
        val automaticButton = radioButtons.find { it.text.contains("Automatic") }!!

        // Select CUSTOM mode
        customButton.doClick()

        // The row containing the text field should be visible
        assertTrue("Text field parent should be visible in CUSTOM mode", isComponentOrParentVisible(textField))

        // Select AUTOMATIC mode
        automaticButton.doClick()

        // The row containing the text field should be hidden
        assertFalse("Text field parent should be hidden in AUTOMATIC mode", isComponentOrParentVisible(textField))
    }

    fun `test switching modes updates isModified correctly`() {
        val configurable = createConfigurable()
        val panel = configurable.createComponent()
        val radioButtons = findAllComponents<JBRadioButton>(panel)

        assertFalse("Initially not modified", configurable.isModified)

        // Change to DISABLED
        radioButtons.find { it.text.contains("Disable") }!!.doClick()
        assertTrue("Modified after changing to DISABLED", configurable.isModified)

        // Change back to AUTOMATIC (original)
        radioButtons.find { it.text.contains("Automatic") }!!.doClick()
        assertFalse("Not modified after changing back to original", configurable.isModified)
    }

    fun `test apply notifies settings changed listeners`() {
        var notified = false
        project.messageBus.connect(testRootDisposable)
            .subscribe(ExpertSettingsListener.TOPIC, ExpertSettingsListener { notified = true })

        val configurable = createConfigurable()
        configurable.createComponent()
        configurable.apply()

        assertTrue("Settings changed listener should be notified", notified)
    }

    private fun isComponentOrParentVisible(component: JComponent): Boolean {
        var current: java.awt.Component? = component
        while (current != null) {
            if (!current.isVisible) return false
            current = current.parent
        }
        return true
    }

    private inline fun <reified T : JComponent> findAllComponents(container: Container): List<T> {
        val result = mutableListOf<T>()
        findAllComponentsRecursive(container, T::class.java, result)
        return result
    }

    private fun <T : JComponent> findAllComponentsRecursive(
        container: Container,
        clazz: Class<T>,
        result: MutableList<T>
    ) {
        for (component in container.components) {
            if (clazz.isInstance(component)) {
                @Suppress("UNCHECKED_CAST")
                result.add(component as T)
            }
            if (component is Container) {
                findAllComponentsRecursive(component, clazz, result)
            }
        }
    }
}
