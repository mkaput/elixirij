package dev.murek.elixirij.lsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem
import dev.murek.elixirij.ExFileType
import dev.murek.elixirij.ExIcons
import dev.murek.elixirij.ExsFileType

class ExpertLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(
        project: Project, file: VirtualFile, serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
        when (file.fileType) {
            ExFileType, ExsFileType -> {
                val expert = Expert.getInstance(project)
                expert.withCurrentExecutableOrRequestSetup {
                    serverStarter.ensureServerStarted(ExpertLspServerDescriptor(project, it))
                }
            }
        }
    }

    override fun createLspServerWidgetItem(lspServer: LspServer, currentFile: VirtualFile?): LspServerWidgetItem =
        // TODO: Link to Expert settings page when it'll be implemented.
        LspServerWidgetItem(lspServer, currentFile, ExIcons.FILE)
}
