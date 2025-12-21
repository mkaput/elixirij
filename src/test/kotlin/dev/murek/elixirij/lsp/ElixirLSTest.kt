package dev.murek.elixirij.lsp

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Files
import kotlin.io.path.deleteExisting
import kotlin.io.path.div

class ElixirLSTest : BasePlatformTestCase() {
    private val elixirLS get() = ElixirLS.getInstance(project)
    private var originalElixirLSMode: ElixirLSMode? = null

    override fun setUp() {
        super.setUp()
        val settings = ExLspSettings.getInstance(project)
        originalElixirLSMode = settings.elixirLSMode
        settings.elixirLSMode = ElixirLSMode.CUSTOM
    }

    override fun tearDown() {
        try {
            val settings = ExLspSettings.getInstance(project)
            originalElixirLSMode?.let { settings.elixirLSMode = it }
        } finally {
            super.tearDown()
        }
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
            try {
                tempFile.deleteExisting()
                tempDir.deleteExisting()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
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
            try {
                tempFile.deleteExisting()
                tempDir.deleteExisting()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}
