package dev.murek.elixirij.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.LspServerSupportProvider.LspServerStarter
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
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
        val downloadManager = service<ExpertDownloadManager>()

        if (!downloadManager.isInstalled()) {
            throw IllegalStateException(
                "Expert language server is not installed. " +
                    "It should be downloaded automatically when opening an Elixir file. " +
                    "Please check your network connection and restart the IDE to trigger another download attempt."
            )
        }

        val expertPath = downloadManager.getExecutablePath()

        return GeneralCommandLine(expertPath.toString(), "--stdio").apply {
            // Set working directory - uses project base path if available, otherwise falls back to user.home
            // This works correctly with multiproject workspaces as each project gets its own LSP server instance
            val workDir = project.basePath ?: System.getProperty("user.home")
            withWorkDirectory(workDir)
        }
    }
}
