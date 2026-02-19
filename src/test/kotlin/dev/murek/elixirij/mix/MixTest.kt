package dev.murek.elixirij.mix

import dev.murek.elixirij.ExSettings
import dev.murek.elixirij.testing.BasePlatformLocalFileTestCase
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.writeText

class MixTest : BasePlatformLocalFileTestCase() {
    private lateinit var settings: ExSettings

    override fun setUp() {
        super.setUp()
        settings = ExSettings.getInstance(project)
    }

    override fun tearDown() {
        try {
            if (::settings.isInitialized) {
                settings.reset()
            }
        } finally {
            super.tearDown()
        }
    }

    fun `test project root for file resolves mix root`() {
        (localProjectRoot / "mix.exs").writeText("defmodule MixProject do end")
        val libDir = (localProjectRoot / "lib").createDirectories()
        val filePath = libDir / "sample.ex"
        filePath.writeText("defmodule Sample do end")

        val virtualFile = refreshAndFindVirtualFile(filePath)
        assertEquals(localProjectRoot, project.mix.projectRootFor(virtualFile))
    }

    fun `test build mix command line uses toolchain and args`() {
        val toolchainRoot = Files.createTempDirectory("elixir-toolchain-")
        val workingDir = Files.createTempDirectory("elixir-mix-working-")
        try {
            val elixir = toolchainRoot / "elixir"
            val elixirc = toolchainRoot / "elixirc"
            val mix = toolchainRoot / "mix"
            val iex = toolchainRoot / "iex"

            elixir.writeText("stub")
            elixirc.writeText("stub")
            mix.writeText("stub")
            iex.writeText("stub")

            elixir.toFile().setExecutable(true)
            elixirc.toFile().setExecutable(true)
            mix.toFile().setExecutable(true)
            iex.toFile().setExecutable(true)

            settings.elixirToolchainPath = elixir.toString()

            val commandLine = project.mix.buildMixCommandLine(
                workingDir,
                "format",
                "--stdin-filename",
                "lib/sample.ex",
                "-",
            )

            assertNotNull(commandLine)
            assertEquals(mix.toString(), commandLine!!.exePath)
            assertEquals(workingDir.toString(), commandLine.workDirectory?.path)
            assertEquals(
                listOf("format", "--stdin-filename", "lib/sample.ex", "-"),
                commandLine.parametersList.parameters,
            )
        } finally {
            toolchainRoot.toFile().deleteRecursively()
            workingDir.toFile().deleteRecursively()
        }
    }
}
