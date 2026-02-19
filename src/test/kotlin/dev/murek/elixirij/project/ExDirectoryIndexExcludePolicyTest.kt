package dev.murek.elixirij.project

import dev.murek.elixirij.testing.BasePlatformLocalFileTestCase
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText

private val EXPECTED_EXCLUDED_DIRECTORY_NAMES = setOf("_build", "deps", ".elixir_ls", ".expert")

class ExDirectoryIndexExcludePolicyTest : BasePlatformLocalFileTestCase() {
    fun `test mix root excludes generated and dependency directories`() {
        (localProjectRoot / "mix.exs").writeText("defmodule MixProject do end")
        refreshAndFindVirtualFile(localProjectRoot / "mix.exs")

        assertEquals(expectedExcludedUrls(projectBasePath, localProjectRoot), excludedUrls())
    }

    fun `test same directory names are not excluded outside mix roots`() {
        (localProjectRoot / "plain").createDirectories()
        assertEquals(emptySet<String>(), excludedUrls())
    }

    fun `test nested mix roots are each handled`() {
        (localProjectRoot / "mix.exs").writeText("defmodule RootMixProject do end")
        refreshAndFindVirtualFile(localProjectRoot / "mix.exs")

        val appOneRoot = (localProjectRoot / "apps" / "app_one").createDirectories()
        val appTwoRoot = (localProjectRoot / "apps" / "app_two").createDirectories()
        (localProjectRoot / "apps" / "plain").createDirectories()

        (appOneRoot / "mix.exs").writeText("defmodule AppOneMixProject do end")
        (appTwoRoot / "mix.exs").writeText("defmodule AppTwoMixProject do end")
        refreshAndFindVirtualFile(appOneRoot / "mix.exs")
        refreshAndFindVirtualFile(appTwoRoot / "mix.exs")

        assertEquals(expectedExcludedUrls(projectBasePath, localProjectRoot, appOneRoot, appTwoRoot), excludedUrls())
    }

    fun `test content root with nested mix apps excludes content root directories`() {
        val appOneRoot = (localProjectRoot / "apps" / "app_one").createDirectories()
        val appTwoRoot = (localProjectRoot / "apps" / "app_two").createDirectories()

        (appOneRoot / "mix.exs").writeText("defmodule AppOneMixProject do end")
        (appTwoRoot / "mix.exs").writeText("defmodule AppTwoMixProject do end")
        refreshAndFindVirtualFile(appOneRoot / "mix.exs")
        refreshAndFindVirtualFile(appTwoRoot / "mix.exs")

        assertEquals(expectedExcludedUrls(projectBasePath, appOneRoot, appTwoRoot), excludedUrls())
    }

    fun `test exclusion applies when excluded directories are created later`() {
        (localProjectRoot / "mix.exs").writeText("defmodule MixProject do end")
        refreshAndFindVirtualFile(localProjectRoot / "mix.exs")

        assertEquals(expectedExcludedUrls(projectBasePath, localProjectRoot), excludedUrls())
    }

    private fun excludedUrls(): Set<String> {
        refreshAndFindVirtualFile(localProjectRoot).refresh(false, true)
        return ExDirectoryIndexExcludePolicy(project).getExcludeUrlsForProject().toSet()
    }

    private fun expectedExcludedUrls(vararg mixRoots: Path): Set<String> = mixRoots
        .distinct()
        .flatMap { mixRoot ->
            val rootUrl = refreshAndFindVirtualFile(mixRoot).url
            EXPECTED_EXCLUDED_DIRECTORY_NAMES.map { excludedName -> "$rootUrl/$excludedName" }
        }
        .toSet()
}
