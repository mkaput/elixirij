package dev.murek.elixirij.lsp

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Startup activity to check and download Expert language server if needed.
 */
class ExpertStartupActivity : ProjectActivity {
    private val logger = Logger.getInstance(ExpertStartupActivity::class.java)
    
    override suspend fun execute(project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val downloadManager = ExpertDownloadManager.getInstance()
            
            if (!downloadManager.isExpertInstalled()) {
                logger.info("Expert language server not found. Starting initial download...")
                downloadManager.downloadAndInstallExpert { success, error ->
                    if (success) {
                        logger.info("Expert language server installed successfully")
                    } else {
                        logger.warn("Failed to install Expert language server: $error")
                    }
                }
            } else {
                // Check for updates in background (once per day)
                downloadManager.checkAndUpdateExpert { success, error ->
                    if (success) {
                        logger.info("Expert language server is up to date")
                    } else {
                        logger.warn("Failed to update Expert language server: $error")
                    }
                }
            }
        }
    }
}
