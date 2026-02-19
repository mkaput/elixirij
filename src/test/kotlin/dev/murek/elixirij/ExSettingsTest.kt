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

    fun `test expert release channel defaults to stable`() {
        val settings = ExSettings.getInstance(project)
        settings.reset()
        assertEquals(ExpertReleaseChannel.STABLE, settings.expertReleaseChannel)
    }

    fun `test expert release channel can be changed`() {
        val settings = ExSettings.getInstance(project)
        settings.expertReleaseChannel = ExpertReleaseChannel.NIGHTLY
        assertEquals(ExpertReleaseChannel.NIGHTLY, settings.expertReleaseChannel)
    }

    fun `test loadState keeps expert disabled in tests`() {
        val settings = ExSettings.getInstance(project)
        val newState = ExSettings.State().apply { expertMode = ExpertMode.AUTOMATIC }

        settings.loadState(newState)

        assertEquals(ExpertMode.DISABLED, settings.expertMode)
    }
}
