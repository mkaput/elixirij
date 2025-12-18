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
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import dev.murek.elixirij.ExBundle
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

private val LOG = logger<Expert>()

/**
 * The entry-point interface for getting the Expert LS instance for a project.
 */
@Service(Service.Level.PROJECT)
class Expert(private val project: Project, private val cs: CoroutineScope) : ExLspServerService {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): Expert = project.service()
    }

    private val settings = ExLspSettings.getInstance(project)

    private val downloading = AtomicBoolean(false)
    val isDownloading: Boolean get() = downloading.get()

    override fun ensureServerStarted(serverStarter: LspServerSupportProvider.LspServerStarter) {
        val executable = currentExecutable()
        check(executable != null)
        LOG.info("Starting Expert from $executable")
        serverStarter.ensureServerStarted(ExpertLspServerDescriptor(project, executable))
    }

    override fun currentExecutable(): Path? = when (settings.expertMode) {
        ExpertMode.AUTOMATIC -> getDownloadedBinaryPath().takeIf { it.exists() }
        ExpertMode.CUSTOM -> settings.expertCustomExecutablePath?.let { Path(it) }?.takeIf { it.exists() }
    }

    override fun checkUpdates() {
        if (settings.expertMode == ExpertMode.AUTOMATIC) {
            val path = getDownloadedBinaryPath()
            if (isStale(path)) {
                downloadExecutable()
            }
        }
    }

    /**
     * Deletes the cached Expert executable.
     */
    fun deleteCachedExecutable() {
        Files.deleteIfExists(getDownloadedBinaryPath())
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

    private fun downloadExecutable() {
        if (!downloading.compareAndSet(false, true)) return

        cs.launch {
            try {
                val assetName = getAssetName()
                if (assetName == null) {
                    LOG.warn("Expert is unsupported on this platform: ${OS.CURRENT} ${CpuArch.CURRENT}, skipping download")

                    @Suppress("DialogTitleCapitalization") NotificationGroupManager.getInstance()
                        .getNotificationGroup("ElixirIJ").createNotification(
                            title = ExBundle.message("lsp.expert.download.task.title"),
                            content = ExBundle.message("lsp.expert.download.unsupported.platform"),
                            type = NotificationType.ERROR
                        ).notify(project)

                    return@launch
                }

                val url = "https://github.com/elixir-lang/expert/releases/download/nightly/$assetName"
                val destination = getDownloadedBinaryPath()
                val isUpdate = destination.exists()

                withBackgroundProgress(project, ExBundle.message("lsp.expert.download.task.title")) {
                    coroutineToIndicator { indicator ->
                        Files.createDirectories(destination.parent)
                        HttpRequests.request(url).connect { request ->
                            val remoteLastModified = request.connection.lastModified
                            if (isUpdate && remoteLastModified != 0L) {
                                val localLastModified = Files.getLastModifiedTime(destination).toMillis()
                                if (remoteLastModified == localLastModified) {
                                    LOG.info("Expert is up to date (timestamp: $remoteLastModified)")
                                    return@connect
                                }
                            }

                            request.saveToFile(destination, indicator)

                            if (!SystemInfo.isWindows) {
                                destination.toFile().setExecutable(true)
                            }
                            if (remoteLastModified != 0L) {
                                Files.setLastModifiedTime(destination, FileTime.fromMillis(remoteLastModified))
                            }
                        }
                    }
                }

                @Suppress("DialogTitleCapitalization") NotificationGroupManager.getInstance()
                    .getNotificationGroup("ElixirIJ").createNotification(
                        title = ExBundle.message("lsp.expert.download.task.title"),
                        content = ExBundle.message("lsp.expert.download.updated"),
                        type = NotificationType.INFORMATION
                    ).notify(project)

                LspServerManager.getInstance(project).stopAndRestartIfNeeded(ExLspServerSupportProvider::class.java)
            } catch (e: Throwable) {
                thisLogger().error("Failed to download expert", e)

            } finally {
                downloading.set(false)
            }
        }
    }

    private fun getDownloadedBinaryPath(): Path =
        PathManager.getSystemDir() / "elixirij" / "expert" / (getAssetName() ?: "expert_unknown_unknown")

    private fun getAssetName(): String? {
        val arch = when (CpuArch.CURRENT) {
            CpuArch.ARM64 -> "arm64"
            CpuArch.X86_64 -> "amd64"
            else -> return null
        }


        return when (OS.CURRENT) {
            OS.Linux -> "expert_linux_${arch}"
            OS.macOS -> "expert_darwin_${arch}"
            OS.Windows -> "expert_windows_amd64.exe"
            else -> null
        }
    }
}
