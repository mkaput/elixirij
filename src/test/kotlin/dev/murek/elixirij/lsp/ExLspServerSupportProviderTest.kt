package dev.murek.elixirij.lsp

import com.intellij.platform.lsp.api.LspServerDescriptor
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.LightVirtualFile
import dev.murek.elixirij.ExSettings
import dev.murek.elixirij.ExpertMode
import dev.murek.elixirij.lang.ExFileType
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class ExLspServerSupportProviderTest : LightPlatformTestCase() {
    private val expert get() = Expert.getInstance(project)

    override fun tearDown() {
        try {
            ExSettings.getInstance(project).reset()
        } finally {
            super.tearDown()
        }
    }

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

    fun `test downloads on first open and starts on next open`() {
        // Intentionally uses the real Expert download; do not replace with a mock.
        val settings = ExSettings.getInstance(project)
        settings.expertMode = ExpertMode.AUTOMATIC

        expert.deleteCached()

        val provider = ExLspServerSupportProvider()
        val file = LightVirtualFile("test.ex", ExFileType, "")
        val starter = CapturingStarter()

        provider.fileOpened(project, file, starter)
        assertFalse(starter.started)

        expert.waitForDownload()

        provider.fileOpened(project, file, starter)
        assertTrue(starter.started)
    }

    private class CapturingStarter : LspServerSupportProvider.LspServerStarter {
        var started = false
            private set

        override fun ensureServerStarted(descriptor: LspServerDescriptor) {
            started = true
        }
    }

    private fun Expert.waitForDownload(deadline: Duration = 60.seconds) = runBlocking {
        val mark = TimeSource.Monotonic.markNow()
        while (mark.elapsedNow() < deadline) {
            val executable = currentExecutable()
            if (executable != null && executable.exists() && !isDownloading) {
                return@runBlocking
            }
            delay(10)
        }

        fail("Expert executable should be downloaded and exist within $deadline")
    }
}
