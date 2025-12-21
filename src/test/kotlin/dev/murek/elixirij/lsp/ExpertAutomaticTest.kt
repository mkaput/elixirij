package dev.murek.elixirij.lsp

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.murek.elixirij.ExBundle
import dev.murek.elixirij.ExSettings
import dev.murek.elixirij.ExpertMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import kotlin.io.path.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class ExpertAutomaticTest : BasePlatformTestCase() {
    private val expert get() = Expert.getInstance(project)

    override fun setUp() {
        super.setUp()
        ExSettings.getInstance(project).expertMode = ExpertMode.AUTOMATIC
    }

    private fun Expert.waitForDownload(deadline: Duration = 60.seconds) = runBlocking {
        val mark = TimeSource.Monotonic.markNow()
        while (mark.elapsedNow() < deadline) {
            val executable = currentExecutable()
            if (executable != null && executable.exists() && !isDownloading) {
                return@runBlocking
            }
            delay(100)
        }

        fail("Expert executable should be downloaded and exist within $deadline")
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
                if (notification.groupId == "ElixirIJ" && notification.title == ExBundle.message("lsp.expert.download.task.title")) {
                    notifications.add(notification)
                }
            }
        })
        return notifications
    }

    fun `test expert download`() {
        expert.deleteCachedExecutable()

        // The download might be already running or finished.
        // To be sure we test the download, we can't easily reset the AtomicBoolean,
        // but we can at least check if it eventually becomes available.
        expert.ensureFresh()
    }

    fun `test expert is NOT updated when fresh`() {
        val oldPath = expert.ensureFresh()

        val notifications = subscribeNotifications()
        val oldLastModified = oldPath.getLastModifiedTime()

        val path = expert.ensureFresh()
        assertEquals(oldLastModified, path.getLastModifiedTime())
        assertEmpty(notifications)
    }

    fun `test expert is updated when stale`() {
        val oldPath = expert.ensureFresh()

        val notifications = subscribeNotifications()
        val oldLastModified = oldPath.getLastModifiedTime()

        expert.markStale()

        val path = expert.ensureFresh()
        assertNotSame(oldLastModified, path.getLastModifiedTime())
        assertNotEmpty(notifications)
    }
}
