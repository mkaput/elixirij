package dev.murek.elixirij.lsp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

private val LOG = logger<ExpertStartupActivity>()

/**
 * Startup activity to check and download Expert language server if needed.
 */
class ExpertStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val downloadManager = ExpertDownloadManager.getInstance()

            if (!downloadManager.isInstalled()) {
                LOG.info("Expert language server not found. Starting initial download...")
                downloadManager.downloadAndInstall { success, error ->
                    if (success) {
                        LOG.info("Expert language server installed successfully")
                    } else {
                        LOG.warn("Failed to install Expert language server: $error")
                    }
                }
            } else {
                // Check for updates in background (limited to once per UPDATE_CHECK_INTERVAL_MS)
                downloadManager.checkAndUpdate { success, error ->
                    if (success) {
                        LOG.info("Expert language server is up to date")
                    } else {
                        LOG.warn("Failed to update Expert language server: $error")
                    }
                }
            }
        }
    }
}
