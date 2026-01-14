package dev.murek.elixirij

import com.intellij.testFramework.LightPlatformTestCase

class ExSettingsTest : LightPlatformTestCase() {

    fun `test expert is disabled in tests`() {
        assertEquals(ExpertMode.DISABLED, ExSettings.getInstance(project).expertMode)
    }
}
