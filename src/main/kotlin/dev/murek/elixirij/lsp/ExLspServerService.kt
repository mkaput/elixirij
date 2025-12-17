package dev.murek.elixirij.lsp

import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerSupportProvider
import java.nio.file.Path

interface ExLspServerService {
    fun ensureServerStarted(serverStarter: LspServerSupportProvider.LspServerStarter)

    /**
     * Returns `true` if this language server is ready to be used.
     *
     * If `true`, then [currentExecutable] **must** return a non-null value.
     * It is crucial that once this service becomes _ready_, it will stay in this state forever.
     */
    fun checkReady(): Boolean = currentExecutable() != null

    /**
     * Get a ready to use language server executable for this project, or `null` if none is available.
     *
     * If returned `null` and you're in an interactive context (e.g., user clicked a button), consider calling
     * [checkUpdates] to let the user download and install the language server.
     */
    fun currentExecutable(): Path?

    /**
     * Trigger update check/fresh install if needed and permitted by the user for this project.
     */
    fun checkUpdates()

    companion object {
        fun getConfiguredServiceInstance(project: Project): ExLspServerService = Expert.getInstance(project)
    }
}
