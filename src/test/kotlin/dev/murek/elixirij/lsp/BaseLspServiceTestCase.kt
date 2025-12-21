package dev.murek.elixirij.lsp

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.setLastModifiedTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

abstract class BaseLspServiceTestCase : BasePlatformTestCase() {
    protected abstract val lspServer: ExLspServerService

    protected val serverName: String
        get() = checkNotNull(lspServer::class.simpleName)

    protected fun ExLspServerService.waitForDownload(deadline: Duration = 60.seconds) = runBlocking {
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
    protected fun ExLspServerService.ensureFresh(): Path {
        checkUpdates()
        waitForDownload()
        val executable = currentExecutable()
        assertNotNull(executable)
        return executable!!
    }

    protected fun ExLspServerService.markStale() {
        currentExecutable()?.setLastModifiedTime(FileTime.fromMillis(0))
    }

    protected fun subscribeNotifications(): List<Notification> {
        val notifications = mutableListOf<Notification>()
        project.messageBus.connect(testRootDisposable).subscribe(Notifications.TOPIC, object : Notifications {
            override fun notify(notification: Notification) {
                if (notification.groupId == "ElixirIJ") {
                    notifications.add(notification)
                }
            }
        })
        return notifications
    }

    fun `test download`() {
        lspServer.deleteCached()

        // The download might be already running or finished.
        // To be sure we test the download, we can't easily reset the AtomicBoolean,
        // but we can at least check if it eventually becomes available.
        lspServer.ensureFresh()
    }

    fun `test is NOT updated when fresh`() {
        val oldPath = lspServer.ensureFresh()

        val notifications = subscribeNotifications()
        val oldLastModified = oldPath.getLastModifiedTime()

        val path = lspServer.ensureFresh()
        assertEquals(oldLastModified, path.getLastModifiedTime())
        assertEmpty(notifications)
    }

    fun `test is updated when stale`() {
        val oldPath = lspServer.ensureFresh()

        val notifications = subscribeNotifications()
        val oldLastModified = oldPath.getLastModifiedTime()

        lspServer.markStale()

        val path = lspServer.ensureFresh()
        assertNotSame(oldLastModified, path.getLastModifiedTime())
        assertNotEmpty(notifications)
    }
}
