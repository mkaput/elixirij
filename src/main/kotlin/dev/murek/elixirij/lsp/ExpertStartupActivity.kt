package dev.murek.elixirij.lsp

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity


/**
 * Startup activity to check and download Expert language server if needed.
 */
class ExpertStartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        val downloadManager = service<ExpertDownloadManager>()
        downloadManager.checkAndUpdate()
    }
}
