package dev.murek.elixirij.lsp

import com.intellij.platform.lsp.api.LspServerDescriptor
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.LightVirtualFile
import dev.murek.elixirij.ExSettings
import dev.murek.elixirij.ExpertMode
import dev.murek.elixirij.lang.ExFileType
import java.nio.file.Files
import kotlin.io.path.deleteIfExists

class ExLspServerSupportProviderTest : LightPlatformTestCase() {
    fun `test starts expert when enabled and ready`() {
        val settings = ExSettings.getInstance(project)
        val executable = Files.createTempFile("expert", ".bin")
        try {
            settings.expertMode = ExpertMode.CUSTOM
            settings.expertCustomExecutablePath = executable.toString()

            val provider = ExLspServerSupportProvider()
            val file = LightVirtualFile("test.ex", ExFileType, "")
            val starter = CapturingStarter()

            provider.fileOpened(project, file, starter)

            assertTrue(starter.started)
        } finally {
            executable.deleteIfExists()
        }
    }

    fun `test does not start expert when disabled`() {
        val settings = ExSettings.getInstance(project)
        settings.expertMode = ExpertMode.DISABLED

        val provider = ExLspServerSupportProvider()
        val file = LightVirtualFile("test.ex", ExFileType, "")
        val starter = CapturingStarter()

        provider.fileOpened(project, file, starter)

        assertFalse(starter.started)
    }

    private class CapturingStarter : LspServerSupportProvider.LspServerStarter {
        var started = false
            private set

        override fun ensureServerStarted(descriptor: LspServerDescriptor) {
            started = true
        }
    }
}
