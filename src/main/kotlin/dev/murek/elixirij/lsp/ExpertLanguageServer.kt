package dev.murek.elixirij.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor

/**
 * LSP server support provider for Expert Language Server.
 */
class ExpertLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter) {
        // Check if this is an Elixir file
        val extension = file.extension
        if (extension == "ex" || extension == "exs") {
            serverStarter.ensureServerStarted(ExpertLspServerDescriptor(project))
        }
    }
}

/**
 * LSP server descriptor for Expert Language Server.
 */
class ExpertLspServerDescriptor(project: Project) : ProjectWideLspServerDescriptor(project, "Expert") {
    
    override fun isSupportedFile(file: VirtualFile): Boolean {
        val extension = file.extension
        return extension == "ex" || extension == "exs"
    }
    
    override fun createCommandLine(): GeneralCommandLine {
        val downloadManager = ExpertDownloadManager.getInstance()
        
        // Ensure Expert is installed
        if (!downloadManager.isExpertInstalled()) {
            throw IllegalStateException("Expert language server is not installed. Please install it from the settings.")
        }
        
        val expertPath = downloadManager.getExpertExecutablePath()
        
        val commandLine = GeneralCommandLine()
        commandLine.exePath = expertPath.toString()
        commandLine.addParameter("lsp")
        
        // Set working directory if available
        val basePath = project.basePath
        if (basePath != null) {
            commandLine.withWorkDirectory(basePath)
        }
        
        return commandLine
    }
}
