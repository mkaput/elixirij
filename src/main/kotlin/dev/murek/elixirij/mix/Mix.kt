package dev.murek.elixirij.mix

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import dev.murek.elixirij.toolchain.elixirToolchain
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Returns the Mix service for this project.
 *
 * This is the preferred way to access the Mix project facilities.
 */
val Project.mix: Mix
    get() = Mix.getInstance(this)

private const val MIX_EXS = "mix.exs"

@Service(Service.Level.PROJECT)
class Mix(private val project: Project) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): Mix = project.service()
    }

    /**
     * Returns the nearest Mix project root for the given file, or null if none can be found.
     */
    fun projectRootFor(virtualFile: VirtualFile?): Path? {
        val projectRoot = project.basePath?.let(::Path) ?: return null
        val filePath = virtualFile
            ?.takeIf { it.isInLocalFileSystem }
            ?.path
            ?.let(::Path)
            ?: return projectRoot.takeIf { (it / MIX_EXS).exists() }


        val start = when {
            filePath.isDirectory() -> filePath
            else -> filePath.parent
        }
            ?.takeIf { it.startsWith(projectRoot) }
            ?: return null

        return generateSequence(start) { it.parent }
            .takeWhile { it.startsWith(projectRoot) }
            .firstOrNull { (it / MIX_EXS).exists() }
    }

    /**
     * Returns the nearest Mix project root for the given file, or null if none can be found.
     */
    fun projectRootFor(file: PsiFile): Path? = projectRootFor(file.virtualFile)

    /**
     * Builds a Mix command line rooted at the given working directory.
     * Returns null if the toolchain is unavailable.
     */
    fun buildMixCommandLine(workingDirectory: Path, vararg args: String): GeneralCommandLine? =
        project.elixirToolchain?.mix?.toString()?.let { mixPath ->
            GeneralCommandLine(mixPath)
                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                .withWorkDirectory(workingDirectory.toString())
                .withParameters(*args)
        }
}
