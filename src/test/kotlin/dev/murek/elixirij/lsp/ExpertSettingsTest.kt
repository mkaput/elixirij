package dev.murek.elixirij.lsp

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ExpertSettingsTest : BasePlatformTestCase() {

    private lateinit var settings: ExpertSettings

    override fun setUp() {
        super.setUp()
        settings = ExpertSettings.getInstance(project)
    }

    fun `test default mode is AUTOMATIC`() {
        assertEquals(ExpertMode.AUTOMATIC, settings.mode)
    }

    fun `test default customExecutablePath is null`() {
        assertNull(settings.customExecutablePath)
    }

    fun `test mode can be changed to DISABLED`() {
        settings.mode = ExpertMode.DISABLED
        assertEquals(ExpertMode.DISABLED, settings.mode)
    }

    fun `test mode can be changed to CUSTOM`() {
        settings.mode = ExpertMode.CUSTOM
        assertEquals(ExpertMode.CUSTOM, settings.mode)
    }

    fun `test customExecutablePath can be set`() {
        settings.customExecutablePath = "/path/to/expert"
        assertEquals("/path/to/expert", settings.customExecutablePath)
    }

    fun `test empty customExecutablePath is normalized to null`() {
        settings.customExecutablePath = ""
        assertNull(settings.customExecutablePath)
    }

    fun `test blank customExecutablePath is normalized to null`() {
        settings.customExecutablePath = "   "
        assertNull(settings.customExecutablePath)
    }

    fun `test customExecutablePath can be reset to null`() {
        settings.customExecutablePath = "/path/to/expert"
        settings.customExecutablePath = null
        assertNull(settings.customExecutablePath)
    }
}
