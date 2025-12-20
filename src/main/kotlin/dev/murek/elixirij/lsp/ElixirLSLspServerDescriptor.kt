package dev.murek.elixirij.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import dev.murek.elixirij.lang.isElixir
import java.nio.file.Path

class ElixirLSLspServerDescriptor(project: Project, private val executable: Path) :
    ProjectWideLspServerDescriptor(project, "ElixirLS") {

    override fun isSupportedFile(file: VirtualFile): Boolean = file.fileType.isElixir

    override fun createCommandLine(): GeneralCommandLine =
        GeneralCommandLine(executable.toString(), "--stdio").withWorkDirectory(project.basePath)
}
