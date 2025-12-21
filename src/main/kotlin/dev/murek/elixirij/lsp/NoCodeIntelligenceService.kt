package dev.murek.elixirij.lsp

import com.intellij.platform.lsp.api.LspServerSupportProvider
import java.nio.file.Path

object NoCodeIntelligenceService : ExLspServerService {
    override fun ensureServerStarted(serverStarter: LspServerSupportProvider.LspServerStarter) {}

    override fun checkReady(): Boolean = false

    override fun currentExecutable(): Path? = null

    override fun checkUpdates() {}

    override val isDownloading: Boolean = false

    override fun deleteCached() {}
}
