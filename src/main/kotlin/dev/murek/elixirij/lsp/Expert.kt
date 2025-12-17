package dev.murek.elixirij.lsp

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerSupportProvider
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

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

    override fun ensureServerStarted(serverStarter: LspServerSupportProvider.LspServerStarter) {
        val executable = currentExecutable()
        check(executable != null)
        serverStarter.ensureServerStarted(ExpertLspServerDescriptor(project, executable))
    }

    override fun currentExecutable(): Path? {
        return when (settings.expertMode) {
            ExpertMode.DISABLED -> null
            ExpertMode.AUTOMATIC -> null // TODO("We don't manage local Expert installs yet.")
            ExpertMode.CUSTOM -> {
                settings.expertCustomExecutablePath?.let { Path(it) }?.takeIf { it.exists() }
            }
        }
    }

    override fun checkUpdates() {
        if (settings.expertMode == ExpertMode.AUTOMATIC) {
            // TODO("We don't manage local Expert installs yet.")
        }
    }
}
