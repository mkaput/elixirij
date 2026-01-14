package dev.murek.elixirij.lsp

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.platform.lsp.api.LspServerDescriptor
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.testFramework.LightPlatformTestCase
import dev.murek.elixirij.ExSettings
import dev.murek.elixirij.ExpertMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.setLastModifiedTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class ExpertTest : LightPlatformTestCase() {
    private val expert get() = Expert.getInstance(project)

    override fun setUp() {
        super.setUp()
        ExSettings.getInstance(project).expertMode = ExpertMode.AUTOMATIC
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
