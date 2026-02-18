package dev.murek.elixirij.project

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.impl.DirectoryIndexExcludePolicy
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile

private val EXCLUDED_DIRECTORY_NAMES = setOf("_build", "deps", ".elixir_ls", ".expert")
private val SKIPPED_DIRECTORY_NAMES = EXCLUDED_DIRECTORY_NAMES + setOf(".git", ".idea")

class ExDirectoryIndexExcludePolicy(private val project: Project) : DirectoryIndexExcludePolicy {
    override fun getExcludeUrlsForProject(): Array<String> {
        val excludeUrls = linkedSetOf<String>()

        for (contentRoot in projectRoots()) {
            collectExcludeUrls(contentRoot, excludeUrls)
        }

        return excludeUrls.toTypedArray()
    }

    private fun projectRoots(): Sequence<VirtualFile> =
        ProjectRootManager.getInstance(project).contentRootsFromAllModules
            .asSequence()
            .distinctBy(VirtualFile::getUrl)

    private fun collectExcludeUrls(contentRoot: VirtualFile, excludeUrls: MutableSet<String>) {
        val stack = ArrayDeque<VirtualFile>()
        val visited = hashSetOf<String>()
        var containsMixProject = false
        stack.addLast(contentRoot)

        while (stack.isNotEmpty()) {
            val directory = stack.removeLast()
            if (!directory.isValid || !directory.isDirectory) continue
            if (!visited.add(directory.path)) continue

            if (directory.findChild("mix.exs") != null) {
                containsMixProject = true
                for (excludedDirectoryName in EXCLUDED_DIRECTORY_NAMES) {
                    excludeUrls.add("${directory.url}/$excludedDirectoryName")
                }
            }

            directory.children
                .asSequence()
                .filter { it.isDirectory }
                .filterNot { it.`is`(VFileProperty.SYMLINK) }
                .filterNot { it.name in SKIPPED_DIRECTORY_NAMES }
                .forEach(stack::addLast)
        }

        if (containsMixProject) {
            for (excludedDirectoryName in EXCLUDED_DIRECTORY_NAMES) {
                excludeUrls.add("${contentRoot.url}/$excludedDirectoryName")
            }
        }
    }
}
