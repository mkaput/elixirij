package dev.murek.elixirij.lsp

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerSupportProvider
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

private val LOG = logger<ElixirLS>()

/**
 * The entry-point interface for getting the ElixirLS instance for a project.
 */
@Service(Service.Level.PROJECT)
class ElixirLS(private val project: Project) : ExLspServerService {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): ElixirLS = project.service()
    }

    private val settings = ExLspSettings.getInstance(project)

    override fun ensureServerStarted(serverStarter: LspServerSupportProvider.LspServerStarter) {
        val executable = currentExecutable()
        check(executable != null)
        LOG.info("Starting ElixirLS from $executable")
        serverStarter.ensureServerStarted(ElixirLsLspServerDescriptor(project, executable))
    }

    override fun currentExecutable(): Path? = when (settings.elixirLSMode) {
        ElixirLSMode.AUTOMATIC -> null // TODO: Automatic mode not yet implemented for ElixirLS
        ElixirLSMode.CUSTOM -> settings.elixirLSCustomExecutablePath?.let { Path(it) }?.takeIf { it.exists() }
    }

    override fun checkUpdates() {
        // ElixirLS doesn't support automatic updates yet
    }
}
