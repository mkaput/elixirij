package dev.murek.elixirij.lsp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.progress.coroutineToIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.ZipUtil
import dev.murek.elixirij.ElixirLSMode
import dev.murek.elixirij.ExBundle
import dev.murek.elixirij.ExSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

private val LOG = logger<ElixirLS>()
private const val RELEASES_API_URL = "https://api.github.com/repos/elixir-lsp/elixir-ls/releases/latest"

/**
 * The entry-point interface for getting the ElixirLS instance for a project.
 */
@Service(Service.Level.PROJECT)
class ElixirLS(private val project: Project, private val cs: CoroutineScope) : ExLspServerService {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): ElixirLS = project.service()
    }

    private val settings = ExSettings.getInstance(project)

    private val downloading = AtomicBoolean(false)
    override val isDownloading: Boolean get() = downloading.get()

    override fun ensureServerStarted(serverStarter: LspServerSupportProvider.LspServerStarter) {
        val executable = currentExecutable()
        check(executable != null)
        LOG.info("Starting ElixirLS from $executable")
        serverStarter.ensureServerStarted(ElixirLSLspServerDescriptor(project, executable))
    }

    override fun currentExecutable(): Path? = when (settings.elixirLSMode) {
        ElixirLSMode.AUTOMATIC -> getLauncherScriptPath().takeIf { it.exists() }
        ElixirLSMode.CUSTOM -> settings.elixirLSCustomExecutablePath?.let { Path(it) }?.takeIf { it.exists() }
    }

    override fun checkUpdates() {
        if (settings.elixirLSMode == ElixirLSMode.AUTOMATIC) {
            val path = getLauncherScriptPath()
            if (isStale(path)) {
                downloadAndExtract()
            }
        }
    }

    override fun deleteCached() {
        val baseDir = getBaseDir()
        if (baseDir.exists()) {
            baseDir.toFile().deleteRecursively()
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun isStale(path: Path): Boolean {
        if (!path.exists()) {
            return true
        }

        val lastModified = path.getLastModifiedTime().toInstant().toKotlinInstant()
        val age = Clock.System.now() - lastModified
        return age > 24.hours
    }

    private fun downloadAndExtract() {
        if (!downloading.compareAndSet(false, true)) return

        cs.launch {
            try {
                val extractDir = getExtractDir()
                val launcher = getLauncherScriptPath()

                val wasUpdated = withBackgroundProgress(project, ExBundle.message("lsp.elixirls.download.task.title")) {
                    coroutineToIndicator { indicator ->
                        // Fetch the latest release info to get the download URL
                        val downloadUrl =
                            HttpRequests.request(RELEASES_API_URL).accept("application/vnd.github.v3+json")
                                .readString(indicator).let { json ->
                                    // Simple JSON parsing to extract the zip asset URL
                                    val regex = """"browser_download_url"\s*:\s*"([^"]+\.zip)"""".toRegex()
                                    regex.find(json)?.groupValues?.get(1)
                                }

                        if (downloadUrl == null) {
                            LOG.warn("Could not find ElixirLS download URL")
                            return@coroutineToIndicator false
                        }

                        val isUpdate = launcher.exists()
                        Files.createDirectories(extractDir.parent)

                        HttpRequests.request(downloadUrl).connect { request ->
                            val remoteLastModified = request.connection.lastModified
                            if (isUpdate && remoteLastModified != 0L) {
                                val localLastModified = Files.getLastModifiedTime(launcher).toMillis()
                                if (remoteLastModified == localLastModified) {
                                    LOG.info("ElixirLS is up to date (timestamp: $remoteLastModified)")
                                    return@connect false
                                }
                            }

                            // Download to a temporary file
                            val tempZip = Files.createTempFile("elixir-ls", ".zip")
                            try {
                                request.saveToFile(tempZip, indicator)

                                // Clean existing extraction directory
                                if (extractDir.exists()) {
                                    extractDir.toFile().deleteRecursively()
                                }
                                Files.createDirectories(extractDir)

                                // Extract the ZIP file
                                ZipUtil.extract(tempZip, extractDir, null)

                                // Make launcher script executable on Unix and set timestamp
                                if (launcher.exists()) {
                                    if (!SystemInfo.isWindows) {
                                        launcher.toFile().setExecutable(true)
                                    }
                                    if (remoteLastModified != 0L) {
                                        Files.setLastModifiedTime(launcher, FileTime.fromMillis(remoteLastModified))
                                    }
                                }
                            } finally {
                                Files.deleteIfExists(tempZip)
                            }
                            true
                        }
                    }
                }

                if (wasUpdated) {
                    @Suppress("DialogTitleCapitalization") NotificationGroupManager.getInstance()
                        .getNotificationGroup("ElixirIJ").createNotification(
                            title = ExBundle.message("lsp.elixirls.download.task.title"),
                            content = ExBundle.message("lsp.elixirls.download.updated"),
                            type = NotificationType.INFORMATION
                        ).notify(project)

                    LspServerManager.getInstance(project).stopAndRestartIfNeeded(ExLspServerSupportProvider::class.java)
                }
            } catch (e: Throwable) {
                thisLogger().error("Failed to download ElixirLS", e)

            } finally {
                downloading.set(false)
            }
        }
    }

    private fun getBaseDir(): Path = PathManager.getSystemDir() / "elixirij" / "elixir-ls"

    private fun getExtractDir(): Path = getBaseDir() / "extracted"

    private fun getLauncherScriptPath(): Path =
        getExtractDir() / if (SystemInfo.isWindows) "language_server.bat" else "language_server.sh"
}
