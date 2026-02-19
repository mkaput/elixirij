package dev.murek.elixirij.lsp

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.platform.lsp.api.LspServerDescriptor
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.testFramework.LightPlatformTestCase
import dev.murek.elixirij.ExSettings
import dev.murek.elixirij.ExpertMode
import dev.murek.elixirij.ExpertReleaseChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.setLastModifiedTime
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * These tests must exercise the real Expert binary download/startup path.
 * Do not replace it with a mock or fake server, as the goal is to detect
 * breakage against current Expert releases.
 */
class ExpertTest : LightPlatformTestCase() {
    private val expert get() = Expert.getInstance(project)

    override fun setUp() {
        super.setUp()
        cleanupBrokenBurritoEntries()
        ExSettings.getInstance(project).apply {
            expertMode = ExpertMode.AUTOMATIC
            expertReleaseChannel = ExpertReleaseChannel.NIGHTLY
        }
    }

    override fun tearDown() {
        try {
            ExSettings.getInstance(project).reset()
        } finally {
            super.tearDown()
        }
    }

    private val serverName: String
        get() = checkNotNull(expert::class.simpleName)

    private fun Expert.waitForDownload(deadline: Duration = 60.seconds) = runBlocking {
        val mark = TimeSource.Monotonic.markNow()
        while (mark.elapsedNow() < deadline) {
            val executable = currentExecutable()
            if (executable != null && executable.exists() && !isDownloading) {
                return@runBlocking
            }
            delay(10)
        }

        fail("$serverName executable should be downloaded and exist within $deadline")
    }

    /**
     * Ensure we have a fresh executable.
     */
    private fun Expert.ensureFresh(): Path {
        checkUpdates()
        waitForDownload()
        val executable = currentExecutable()
        assertNotNull(executable)
        return executable!!
    }

    private fun Expert.markStale() {
        currentExecutable()?.setLastModifiedTime(FileTime.fromMillis(0))
    }

    private fun subscribeNotifications(): List<Notification> {
        val notifications = mutableListOf<Notification>()
        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                if (notification.groupId == "Elixirij") {
                    notifications.add(notification)
                }
            }
        })
        return notifications
    }

    private fun Expert.getDescriptor(): LspServerDescriptor {
        lateinit var captured: LspServerDescriptor
        ensureServerStarted(object : LspServerSupportProvider.LspServerStarter {
            override fun ensureServerStarted(descriptor: LspServerDescriptor) {
                captured = descriptor
            }
        })
        return captured
    }

    /**
     * Remove incomplete Expert Burrito runtime directories that are missing `_metadata.json`.
     *
     * Expert uses Burrito to manage its runtime. If a previous download was interrupted,
     * Burrito can leave an empty runtime directory without metadata. In that state, Expert
     * fails to boot and the LSP handshake times out. This cleanup keeps the test focused on
     * verifying the real Expert binary startup rather than being derailed by stale runtimes.
     */
    private fun cleanupBrokenBurritoEntries() {
        val home = System.getProperty("user.home")
        val candidates = listOf(
            Path(home, "Library", "Application Support", ".burrito"),
            Path(home, ".burrito"),
        )

        for (root in candidates) {
            if (!root.exists()) continue
            Files.list(root).use { entries ->
                entries.filter { it.fileName.name.startsWith("expert_erts-") }
                    .forEach { entry ->
                        val metadata = entry.resolve("_metadata.json")
                        if (!metadata.exists()) {
                            entry.toFile().deleteRecursively()
                        }
                    }
            }
        }
    }

    fun `test download`() {
        expert.deleteCached()

        // The download might be already running or finished.
        // To be sure we test the download, we can't easily reset the AtomicBoolean,
        // but we can at least check if it eventually becomes available.
        expert.ensureFresh()
    }

    fun `test is NOT updated when fresh`() {
        val oldPath = expert.ensureFresh()

        val notifications = subscribeNotifications()
        val oldLastModified = oldPath.getLastModifiedTime()

        val path = expert.ensureFresh()
        assertEquals(oldLastModified, path.getLastModifiedTime())
        assertEmpty(notifications)
    }

    fun `test is updated when stale`() {
        val oldPath = expert.ensureFresh()

        val notifications = subscribeNotifications()
        val oldLastModified = oldPath.getLastModifiedTime()

        expert.markStale()

        val path = expert.ensureFresh()
        assertNotSame(oldLastModified, path.getLastModifiedTime())
        assertNotEmpty(notifications)
    }

    fun `test server verification returns server info`() {
        // Intentionally uses the real Expert binary; do not mock this.
        // Ensure project base path exists (it may have been cleaned up between tests)
        project.basePath?.let { Files.createDirectories(Path.of(it)) }

        expert.ensureFresh()
        val descriptor = expert.getDescriptor()
        val result = runBlocking {
            verifyLspServer(descriptor, timeout = 15.seconds)
        }
        val serverInfo = result.getOrThrow()
        println("Server info: $serverInfo")
    }
}
