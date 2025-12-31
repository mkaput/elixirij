package dev.murek.elixirij

import com.intellij.testFramework.LightPlatformTestCase

class ExSettingsTest : LightPlatformTestCase() {

    fun `test default code intelligence service is none in tests`() {
        assertEquals(CodeIntelligenceService.NONE, ExSettings.getInstance(project).codeIntelligenceService)
    }
}
