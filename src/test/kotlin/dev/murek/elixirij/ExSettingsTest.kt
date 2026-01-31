package dev.murek.elixirij

import com.intellij.testFramework.LightPlatformTestCase

class ExSettingsTest : LightPlatformTestCase() {
    override fun setUp() {
        super.setUp()
        ExSettings.getInstance(project).reset()
    }

    fun `test expert is disabled in tests`() {
        val settings = ExSettings.getInstance(project)
        settings.reset()
        assertEquals(ExpertMode.DISABLED, settings.expertMode)
    }
}
