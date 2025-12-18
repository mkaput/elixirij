package dev.murek.elixirij.lsp

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.thisLogger
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists

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

    override fun ensureServerStarted(serverStarter: LspServerSupportProvider.LspServerStarter) {
        val executable = currentExecutable()
        check(executable != null)
        serverStarter.ensureServerStarted(ExpertLspServerDescriptor(project, executable))
    }

    override fun currentExecutable(): Path? {
        return when (settings.expertMode) {
            ExpertMode.DISABLED -> null
            ExpertMode.AUTOMATIC -> getDownloadedBinaryPath().takeIf { it.exists() }
            ExpertMode.CUSTOM -> {
                settings.expertCustomExecutablePath?.let { Path(it) }?.takeIf { it.exists() }
            }
        }
    }

    override fun checkUpdates() {
        if (settings.expertMode == ExpertMode.AUTOMATIC) {
            if (currentExecutable() == null) {
                downloadExecutable()
            }
        }
    }

    private fun downloadExecutable() {
        if (!downloading.compareAndSet(false, true)) return

        cs.launch {
            try {
                val assetName = getAssetName()
                if (assetName == null) {
                    LOG.warn("expert is unsupported on this platform: ${OS.CURRENT} ${CpuArch.CURRENT}, skipping download")

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

                withBackgroundProgress(project, ExBundle.message("lsp.expert.download.task.title")) {
                    withContext(Dispatchers.IO) {
                        Files.createDirectories(destination.parent)
                        HttpRequests.request(url).saveToFile(destination, null)

                        if (!SystemInfo.isWindows) {
                            destination.toFile().setExecutable(true)
                        }
                    }
                }

                LspServerManager.getInstance(project).stopAndRestartIfNeeded(ExLspServerSupportProvider::class.java)
            } catch (e: Throwable) {
                thisLogger().error("failed to download expert", e)

            } finally {
                downloading.set(false)
            }
        }
    }

    private fun getDownloadedBinaryPath(): Path =
        PathManager.getCommonDataPath() / "elixirij" / "expert" / (getAssetName() ?: "expert_unknown_unknown")

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
