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
import com.intellij.util.io.HttpRequests.HttpStatusException
import com.intellij.util.system.CpuArch
import com.intellij.util.system.OS
import dev.murek.elixirij.ExBundle
import dev.murek.elixirij.ExSettings
import dev.murek.elixirij.ExpertMode
import dev.murek.elixirij.ExpertReleaseChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.toKotlinInstant

private val LOG = logger<Expert>()
private const val EXPERT_RELEASES_BASE_URL = "https://github.com/elixir-lang/expert/releases"
private const val EXPERT_RELEASES_API_URL = "https://api.github.com/repos/elixir-lang/expert/releases?per_page=20"

private val ExpertReleaseChannel.cacheDirectoryName: String
    get() = when (this) {
        ExpertReleaseChannel.STABLE -> "stable"
        ExpertReleaseChannel.NIGHTLY -> "nightly"
    }

private fun ExpertReleaseChannel.downloadUrl(assetName: String): String = when (this) {
    ExpertReleaseChannel.STABLE -> "$EXPERT_RELEASES_BASE_URL/latest/download/$assetName"
    ExpertReleaseChannel.NIGHTLY -> "$EXPERT_RELEASES_BASE_URL/download/nightly/$assetName"
}

/**
 * The entry-point interface for getting the Expert LS instance for a project.
 */
@Service(Service.Level.PROJECT)
class Expert(private val project: Project, private val cs: CoroutineScope) {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): Expert = project.service()
    }

    private val settings = ExSettings.getInstance(project)

    private val downloading = AtomicBoolean(false)

    /**
     * Returns `true` if the language server is currently downloading.
     */
    val isDownloading: Boolean get() = downloading.get()

    /**
     * Starts Expert for the project if a ready executable is available.
     */
    fun ensureServerStarted(serverStarter: LspServerSupportProvider.LspServerStarter) {
        val executable = currentExecutable() ?: return
        LOG.info("Starting Expert from $executable")
        serverStarter.ensureServerStarted(ExpertLspServerDescriptor(project, executable))
    }

    /**
     * Returns `true` if Expert is ready to be used.
     *
     * If `true`, then [currentExecutable] **must** return a non-null value.
     * It is crucial that once Expert becomes _ready_, it will stay in this state forever.
     */
    fun checkReady(): Boolean = !isDownloading && currentExecutable() != null

    /**
     * Get a ready to use Expert executable for this project, or `null` if none is available.
     *
     * If returned `null` and you're in an interactive context (e.g., user clicked a button),
     * consider calling [checkUpdates] to let the user download and install Expert.
     */
    fun currentExecutable(): Path? = when (settings.expertMode) {
        ExpertMode.DISABLED -> null
        ExpertMode.AUTOMATIC -> currentAutomaticExecutable()
        ExpertMode.CUSTOM -> settings.expertCustomExecutablePath?.let { Path(it) }?.takeIf { it.exists() }
    }

    /**
     * Trigger an update check/fresh install if needed and permitted by the user for this project.
     */
    fun checkUpdates(force: Boolean = false) {
        if (settings.expertMode != ExpertMode.AUTOMATIC) return

        val channel = settings.expertReleaseChannel
        val path = getDownloadedBinaryPath(channel) ?: return
        if (force || isStale(path)) {
            downloadExecutable(channel)
        }
    }

    /**
     * Deletes the cached Expert executable.
     */
    fun deleteCached() {
        ExpertReleaseChannel.entries.forEach { channel ->
            getDownloadedBinaryPath(channel)?.let { Files.deleteIfExists(it) }
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

    private fun currentAutomaticExecutable(channel: ExpertReleaseChannel = settings.expertReleaseChannel): Path? {
        val assetName = getAssetName() ?: return null
        return sequenceOf(channel)
            .plus(ExpertReleaseChannel.entries.filter { it != channel })
            .map { selectedChannel -> getDownloadedBinaryPath(selectedChannel, assetName) }
            .firstOrNull { it.exists() }
    }

    private fun downloadExecutable(channel: ExpertReleaseChannel) {
        if (!downloading.compareAndSet(false, true)) return

        cs.launch {
            try {
                val assetName = getAssetName()
                if (assetName == null) {
                    LOG.warn("Expert is unsupported on this platform: ${OS.CURRENT} ${CpuArch.CURRENT}, skipping download")

                    @Suppress("DialogTitleCapitalization") NotificationGroupManager.getInstance()
                        .getNotificationGroup("Elixirij").createNotification(
                            title = ExBundle.message("lsp.expert.download.task.title"),
                            content = ExBundle.message("lsp.expert.download.unsupported.platform"),
                            type = NotificationType.ERROR
                        ).notify(project)

                    return@launch
                }

                val url = channel.downloadUrl(assetName)
                val destination = getDownloadedBinaryPath(channel, assetName)
                val isUpdate = destination.exists()
                var wasDownloaded = false

                withBackgroundProgress(project, ExBundle.message("lsp.expert.download.task.title")) {
                    coroutineToIndicator { indicator ->
                        Files.createDirectories(destination.parent)
                        fun downloadFromUrl(url: String): Boolean {
                            var downloaded = false
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

                                if (destination.exists()) {
                                    if (!SystemInfo.isWindows) {
                                        destination.toFile().setExecutable(true)
                                    }
                                    if (remoteLastModified != 0L) {
                                        Files.setLastModifiedTime(destination, FileTime.fromMillis(remoteLastModified))
                                    }
                                    downloaded = true
                                }
                            }
                            return downloaded
                        }

                        try {
                            wasDownloaded = downloadFromUrl(url)
                        } catch (e: HttpStatusException) {
                            if (channel == ExpertReleaseChannel.STABLE && e.statusCode == 404) {
                                val fallbackTag = fetchLatestTaggedReleaseTag()
                                val fallbackUrl = "$EXPERT_RELEASES_BASE_URL/download/$fallbackTag/$assetName"
                                wasDownloaded = downloadFromUrl(fallbackUrl)
                                return@coroutineToIndicator
                            }
                            throw e
                        }
                    }
                }

                if (!wasDownloaded) {
                    return@launch
                }

                @Suppress("DialogTitleCapitalization") NotificationGroupManager.getInstance()
                    .getNotificationGroup("Elixirij").createNotification(
                        title = ExBundle.message("lsp.expert.download.task.title"),
                        content = ExBundle.message("lsp.expert.download.updated"),
                        type = NotificationType.INFORMATION
                    ).notify(project)

                LspServerManager.getInstance(project).stopAndRestartIfNeeded(ExLspServerSupportProvider::class.java)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                thisLogger().error("Failed to download expert", e)

            } finally {
                downloading.set(false)
            }
        }
    }

    private fun fetchLatestTaggedReleaseTag(): String =
        HttpRequests.request(EXPERT_RELEASES_API_URL).connect { request ->
            Regex(""""tag_name"\s*:\s*"([^"]+)"""")
                .findAll(request.readString())
                .map { it.groupValues[1] }
                .firstOrNull { it != "nightly" }
                ?: throw IllegalStateException("No fallback Expert release tag found")
        }

    private fun getDownloadedBinaryPath(channel: ExpertReleaseChannel): Path? =
        getAssetName()?.let { getDownloadedBinaryPath(channel, it) }

    private fun getDownloadedBinaryPath(channel: ExpertReleaseChannel, assetName: String): Path =
        PathManager.getSystemDir() / "elixirij" / "expert" / channel.cacheDirectoryName / assetName

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
