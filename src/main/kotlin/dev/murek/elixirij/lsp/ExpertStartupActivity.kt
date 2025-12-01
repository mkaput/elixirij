package dev.murek.elixirij.lsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity


/**
 * Startup activity to check and download the Expert language server if needed.
 */
class ExpertStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        Expert.getInstance(project).checkUpdates()
    }
}
