package dev.murek.elixirij.ide.formatter

import com.intellij.formatting.FormatTextRanges
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import dev.murek.elixirij.ExSettings
import dev.murek.elixirij.lang.ExFile
import dev.murek.elixirij.testing.BasePlatformLocalFileTestCase
import dev.murek.elixirij.toolchain.elixirToolchain
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Integration-style tests for [ExMixFormatService] without invoking a real `mix` binary.
 *
 * The formatter service ultimately shells out to `mix format`. In tests, we emulate that by:
 * - Creating a fake Elixir toolchain in a temp directory (elixir/elixirc/iex/mix stubs).
 * - Pointing [ExSettings.elixirToolchainPath] at the fake toolchain so the service resolves it.
 * - Replacing the `mix` script content per test to control stdout/stderr and the formatted output.
 *
 * We call the registered [ExMixFormatService] directly to avoid @ApiStatus.Internal APIs
 * while still exercising the same formatRanges entry point used by the platform.
 */
class ExMixFormatServiceTest : BasePlatformLocalFileTestCase() {

    private lateinit var libDir: Path
    private lateinit var toolchainRoot: Path
    private lateinit var settings: ExSettings
    private lateinit var elixir: Path
    private lateinit var elixirc: Path
    private lateinit var mix: Path
    private lateinit var iex: Path

    override fun setUp() {
        super.setUp()
        (localProjectRoot / "mix.exs").writeText("defmodule MixProject do end")
        libDir = (localProjectRoot / "lib").createDirectories()

        toolchainRoot = Files.createTempDirectory("elixir-toolchain-")
        settings = ExSettings.getInstance(project)
        settings.reset()

        ensureToolchainFiles()

        settings.elixirToolchainPath = elixir.toString()
    }

    override fun tearDown() {
        try {
            if (::settings.isInitialized) {
                settings.reset()
            }
            if (::toolchainRoot.isInitialized) {
                toolchainRoot.toFile().deleteRecursively()
            }
        } finally {
            super.tearDown()
        }
    }

    fun `test mix format returns formatted text`() {
        val sourcePath = libDir / "simple.ex"
        sourcePath.writeText("unformatted")

        val documentText = formatFile(sourcePath)
        assertEquals("formatted", documentText)
    }

    fun `test mix format ignores stdout`() {
        val sourcePath = libDir / "sample.ex"
        sourcePath.writeText("unformatted")

        ensureToolchainFiles()
        mix.writeText(
            $$"""
            #!/bin/sh
            if [ "$1" = "format" ]; then
              file="$2"
              echo "==> libwbxml"
              echo "-- Configuring done"
              printf "formatted" > "$file"
              exit 0
            fi
            exit 1
            """.trimIndent()
        )

        val documentText = formatFile(sourcePath)
        assertEquals("formatted", documentText)
    }

    fun `test mix format runs on partial ranges`() {
        val sourcePath = libDir / "partial.ex"
        sourcePath.writeText("unformatted")

        val documentText = formatFile(sourcePath, listOf(TextRange(0, 1)))
        assertEquals("formatted", documentText)
    }

    private fun formatFile(sourcePath: Path, ranges: List<TextRange>? = null): String {
        ensureToolchainFiles()
        checkNotNull(project.elixirToolchain)

        val virtualFile = refreshAndFindVirtualFile(sourcePath)

        val psiFile = checkNotNull(PsiManager.getInstance(project).findFile(virtualFile))
        assertTrue(psiFile is ExFile)
        val document = checkNotNull(PsiDocumentManager.getInstance(project).getDocument(psiFile))
        PsiDocumentManager.getInstance(project).commitDocument(document)
        assertEquals(document.textLength, psiFile.textRange.length)

        WriteCommandAction.runWriteCommandAction(project) {
            val formatRanges = if (ranges.isNullOrEmpty()) {
                FormatTextRanges(psiFile.textRange, false)
            } else {
                ranges.fold(FormatTextRanges()) { acc, range ->
                    acc.apply { add(range, true) }
                }
            }
            findMixFormatService().formatRanges(psiFile, formatRanges, false, false)
        }

        return document.text
    }

    private fun findMixFormatService(): FormattingService =
        checkNotNull(
            FormattingService.EP_NAME.extensionList.firstOrNull { it is ExMixFormatService }
        ) { "ExMixFormatService should be registered" }

    private fun ensureToolchainFiles() {
        settings.elixirToolchainPath?.let { configured ->
            toolchainRoot = Path.of(configured).parent
        }
        if (!toolchainRoot.exists()) {
            toolchainRoot = toolchainRoot.createDirectories()
        }

        elixir = toolchainRoot / "elixir"
        elixirc = toolchainRoot / "elixirc"
        mix = toolchainRoot / "mix"
        iex = toolchainRoot / "iex"

        if (!elixir.exists()) elixir.writeText("stub")
        if (!elixirc.exists()) elixirc.writeText("stub")
        if (!mix.exists()) {
            mix.writeText(
                $$"""
                #!/bin/sh
                if [ "$1" = "format" ]; then
                  file="$2"
                  printf "formatted" > "$file"
                  exit 0
                fi
                exit 1
                """.trimIndent()
            )
        }
        if (!iex.exists()) iex.writeText("stub")

        elixir.toFile().setExecutable(true)
        elixirc.toFile().setExecutable(true)
        mix.toFile().setExecutable(true)
        iex.toFile().setExecutable(true)
    }
}
