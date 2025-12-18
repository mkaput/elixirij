package dev.murek.elixirij.lsp

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class ExpertDownloadTest : BasePlatformTestCase() {
    fun `test expert download`() = runBlocking {
        val expert = Expert.getInstance(project)
        val settings = ExLspSettings.getInstance(project)

        settings.expertMode = ExpertMode.AUTOMATIC

        // The download might be already running or finished.
        // To be sure we test the download, we can't easily reset the AtomicBoolean,
        // but we can at least check if it eventually becomes available.
        expert.checkUpdates()

        val deadline = 60.seconds
        val mark = TimeSource.Monotonic.markNow()
        while (mark.elapsedNow() < deadline) {
            if (expert.currentExecutable()?.takeIf { it.exists() } != null) {
                return@runBlocking
            }
            delay(100)
        }

        fail("Expert executable should be downloaded and exist within $deadline")
    }
}
