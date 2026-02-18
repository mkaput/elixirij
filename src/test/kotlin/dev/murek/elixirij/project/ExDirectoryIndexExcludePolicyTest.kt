package dev.murek.elixirij.project

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import dev.murek.elixirij.testing.fixtureName
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText

private val EXPECTED_EXCLUDED_DIRECTORY_NAMES = setOf("_build", "deps", ".elixir_ls", ".expert")

class ExDirectoryIndexExcludePolicyTest : LightPlatformTestCase() {
    private lateinit var projectRoot: Path

    override fun setUp() {
        super.setUp()
        val basePath = Path.of(checkNotNull(project.basePath)).createDirectories()
        projectRoot = (basePath / "exclude-policy-$fixtureName").createDirectories()
    }

    override fun tearDown() {
        try {
            if (::projectRoot.isInitialized) {
                cleanupTestArtifacts()
            }
        } finally {
            super.tearDown()
        }
    }

    fun `test mix root excludes generated and dependency directories`() {
        (projectRoot / "mix.exs").writeText("defmodule MixProject do end")
        refreshVirtualFile(projectRoot / "mix.exs")

        assertEquals(expectedExcludedUrls(projectContentRoot(), projectRoot), excludedUrls())
    }

    fun `test same directory names are not excluded outside mix roots`() {
        (projectRoot / "plain").createDirectories()
        assertEquals(emptySet<String>(), excludedUrls())
    }

    fun `test nested mix roots are each handled`() {
        (projectRoot / "mix.exs").writeText("defmodule RootMixProject do end")
        refreshVirtualFile(projectRoot / "mix.exs")

        val appOneRoot = (projectRoot / "apps" / "app_one").createDirectories()
        val appTwoRoot = (projectRoot / "apps" / "app_two").createDirectories()
        val plainRoot = (projectRoot / "apps" / "plain").createDirectories()

        (appOneRoot / "mix.exs").writeText("defmodule AppOneMixProject do end")
        (appTwoRoot / "mix.exs").writeText("defmodule AppTwoMixProject do end")
        refreshVirtualFile(appOneRoot / "mix.exs")
        refreshVirtualFile(appTwoRoot / "mix.exs")

        assertEquals(expectedExcludedUrls(projectContentRoot(), projectRoot, appOneRoot, appTwoRoot), excludedUrls())
    }

    fun `test content root with nested mix apps excludes content root directories`() {
        val appOneRoot = (projectRoot / "apps" / "app_one").createDirectories()
        val appTwoRoot = (projectRoot / "apps" / "app_two").createDirectories()

        (appOneRoot / "mix.exs").writeText("defmodule AppOneMixProject do end")
        (appTwoRoot / "mix.exs").writeText("defmodule AppTwoMixProject do end")
        refreshVirtualFile(appOneRoot / "mix.exs")
        refreshVirtualFile(appTwoRoot / "mix.exs")

        assertEquals(expectedExcludedUrls(projectContentRoot(), appOneRoot, appTwoRoot), excludedUrls())
    }

    fun `test exclusion applies when excluded directories are created later`() {
        (projectRoot / "mix.exs").writeText("defmodule MixProject do end")
        refreshVirtualFile(projectRoot / "mix.exs")

        assertEquals(expectedExcludedUrls(projectContentRoot(), projectRoot), excludedUrls())
    }

    private fun excludedUrls(): Set<String> {
        findVirtualFile(projectRoot).refresh(false, true)
        return ExDirectoryIndexExcludePolicy(project).getExcludeUrlsForProject().toSet()
    }

    private fun expectedExcludedUrls(vararg mixRoots: Path): Set<String> = mixRoots
        .distinct()
        .flatMap { mixRoot ->
            val rootUrl = findVirtualFile(mixRoot).url
            EXPECTED_EXCLUDED_DIRECTORY_NAMES.map { excludedName -> "$rootUrl/$excludedName" }
        }
        .toSet()

    private fun projectContentRoot(): Path = Path.of(checkNotNull(project.basePath))

    private fun findVirtualFile(path: Path): VirtualFile = checkNotNull(
        LocalFileSystem.getInstance().refreshAndFindFileByPath(path.toString())
    ) { "Virtual file should be found for $path" }

    private fun refreshVirtualFile(path: Path): VirtualFile = findVirtualFile(path)

    private fun cleanupTestArtifacts() {
        if (projectRoot.toFile().exists()) {
            projectRoot.toFile().deleteRecursively()
        }

        LocalFileSystem.getInstance().refreshAndFindFileByPath(Path.of(checkNotNull(project.basePath)).toString())
            ?.refresh(false, true)
    }
}
