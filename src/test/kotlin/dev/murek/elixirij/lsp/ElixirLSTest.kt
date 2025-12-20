package dev.murek.elixirij.lsp

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Files
import kotlin.io.path.div

class ElixirLSTest : BasePlatformTestCase() {
    private val elixirLS get() = ElixirLS.getInstance(project)

    override fun setUp() {
        super.setUp()
        ExLspSettings.getInstance(project).elixirLSMode = ElixirLSMode.CUSTOM
    }

    fun `test elixirls returns null when no custom path configured`() {
        ExLspSettings.getInstance(project).elixirLSCustomExecutablePath = null
        assertNull(elixirLS.currentExecutable())
    }

    fun `test elixirls returns null when custom path does not exist`() {
        ExLspSettings.getInstance(project).elixirLSCustomExecutablePath = "/non/existent/path"
        assertNull(elixirLS.currentExecutable())
    }

    fun `test elixirls returns path when custom path exists`() {
        val tempDir = Files.createTempDirectory("elixirls-test")
        val tempFile = tempDir / "language_server.sh"
        Files.createFile(tempFile)

        try {
            ExLspSettings.getInstance(project).elixirLSCustomExecutablePath = tempFile.toString()
            val executable = elixirLS.currentExecutable()
            assertNotNull(executable)
            assertEquals(tempFile, executable)
        } finally {
            Files.deleteIfExists(tempFile)
            Files.deleteIfExists(tempDir)
        }
    }

    fun `test elixirls checkReady returns false when no executable`() {
        ExLspSettings.getInstance(project).elixirLSCustomExecutablePath = null
        assertFalse(elixirLS.checkReady())
    }

    fun `test elixirls checkReady returns true when executable exists`() {
        val tempDir = Files.createTempDirectory("elixirls-test")
        val tempFile = tempDir / "language_server.sh"
        Files.createFile(tempFile)

        try {
            ExLspSettings.getInstance(project).elixirLSCustomExecutablePath = tempFile.toString()
            assertTrue(elixirLS.checkReady())
        } finally {
            Files.deleteIfExists(tempFile)
            Files.deleteIfExists(tempDir)
        }
    }
}
