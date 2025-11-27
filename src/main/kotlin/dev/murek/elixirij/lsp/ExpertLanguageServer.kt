package dev.murek.elixirij.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.LspServerSupportProvider.LspServerStarter
import dev.murek.elixirij.ExFileType

/**
 * LSP server support provider for Expert Language Server.
 */
class ExpertLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerStarter) {
        if (file.fileType == ExFileType) {
            serverStarter.ensureServerStarted(ExpertLspServerDescriptor(project))
        }
    }
}

/**
 * LSP server descriptor for Expert Language Server.
 */
class ExpertLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Expert") {
    
    override fun isSupportedFile(file: VirtualFile): Boolean {
        return file.fileType == ExFileType
    }
    
    override fun createCommandLine(): GeneralCommandLine {
        val downloadManager = ExpertDownloadManager.getInstance()
        
        // Trigger download if not installed and wait
        if (!downloadManager.isExpertInstalled()) {
            var downloadComplete = false
            var downloadError: String? = null
            
            downloadManager.downloadAndInstallExpert { success, error ->
                downloadComplete = true
                if (!success) {
                    downloadError = error
                }
            }
            
            // Wait for download with timeout
            val timeout = System.currentTimeMillis() + 60000 // 60 second timeout
            while (!downloadComplete && System.currentTimeMillis() < timeout) {
                Thread.sleep(100)
            }
            
            if (downloadError != null) {
                throw IllegalStateException("Failed to download Expert: $downloadError")
            }
            
            if (!downloadManager.isExpertInstalled()) {
                throw IllegalStateException("Expert language server is not installed. Download timed out.")
            }
        }
        
        val expertPath = downloadManager.getExpertExecutablePath()
        
        return GeneralCommandLine(expertPath.toString(), "lsp").apply {
            // Set working directory - uses project base path
            // This works correctly with multiproject workspaces as each project gets its own LSP server instance
            project.basePath?.let { withWorkDirectory(it) }
        }
    }
}
