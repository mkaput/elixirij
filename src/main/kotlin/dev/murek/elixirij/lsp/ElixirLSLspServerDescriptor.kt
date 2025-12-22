package dev.murek.elixirij.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import dev.murek.elixirij.lang.isElixir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists

class ElixirLSLspServerDescriptor(project: Project, private val executable: Path) :
    ProjectWideLspServerDescriptor(project, "ElixirLS") {

    override fun isSupportedFile(file: VirtualFile): Boolean = file.fileType.isElixir

    override fun createCommandLine(): GeneralCommandLine =
        GeneralCommandLine(executable.toString())
            .withWorkDirectory(project.basePath)
            .withEnvironment(buildEnv())

    private fun buildEnv(): Map<String, String> = buildMap {
        val home = System.getenv("HOME") ?: System.getProperty("user.home")
        put("HOME", home)

        // Build PATH with asdf installations if available
        val pathParts = mutableListOf<String>()

        // Add asdf Elixir and Erlang bin directories
        val asdfDataDir = Path.of(home, ".asdf")
        if (asdfDataDir.exists()) {
            findLatestAsdfInstall(asdfDataDir, "elixir")?.let { pathParts.add("$it/bin") }
            findLatestAsdfInstall(asdfDataDir, "erlang")?.let { pathParts.add("$it/bin") }
        }

        // Append system PATH
        System.getenv("PATH")?.let { pathParts.add(it) }

        put("PATH", pathParts.joinToString(File.pathSeparator))
    }

    private fun findLatestAsdfInstall(asdfDataDir: Path, tool: String): Path? {
        val installsDir = asdfDataDir.resolve("installs").resolve(tool)
        if (!installsDir.exists()) return null

        return installsDir.toFile().listFiles()
            ?.filter { it.isDirectory }
            ?.maxByOrNull { it.name }
            ?.toPath()
    }
}
