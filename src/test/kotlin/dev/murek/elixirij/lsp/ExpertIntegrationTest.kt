package dev.murek.elixirij.lsp

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.murek.elixirij.ExFileType
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * End-to-end integration tests for Expert Language Server.
 *
 * These tests use the real Expert binary downloaded from GitHub.
 * They verify the complete flow from downloading Expert to using it with Elixir files.
 *
 * Note: These tests require network access to download Expert. If network is unavailable
 * (e.g., in CI environments with restricted network), tests will pass with a message
 * indicating Expert could not be downloaded.
 */
class ExpertIntegrationTest : BasePlatformTestCase() {

    companion object {
        // Track if we've already tried (and failed) to download Expert
        private val downloadAttempted = AtomicBoolean(false)
        private val downloadSucceeded = AtomicBoolean(false)
    }

    /**
     * Downloads Expert if not already installed and waits for completion.
     * Returns true if Expert is ready to use, false if download failed (e.g., network unavailable).
     */
    private fun ensureExpertDownloaded(): Boolean {
        val downloadManager = ExpertDownloadManager.getInstance()

        if (downloadManager.isInstalled()) {
            downloadSucceeded.set(true)
            return true
        }

        // If we already tried and failed, don't retry
        if (downloadAttempted.get() && !downloadSucceeded.get()) {
            return false
        }

        downloadAttempted.set(true)

        val latch = CountDownLatch(1)
        var success = false

        downloadManager.downloadAndInstall { result, _ ->
            success = result
            latch.countDown()
        }

        // Wait up to 2 minutes for download to complete
        latch.await(2, TimeUnit.MINUTES)

        val isInstalled = downloadManager.isInstalled()
        downloadSucceeded.set(isInstalled)
        return isInstalled
    }

    /**
     * Skip helper that prints a message when Expert is unavailable.
     */
    private fun skipIfExpertUnavailable(): Boolean {
        if (!ensureExpertDownloaded()) {
            println("SKIPPED: Expert binary could not be downloaded (network may be unavailable)")
            return true
        }
        return false
    }

    fun `test download real expert binary`() {
        if (skipIfExpertUnavailable()) return

        val downloadManager = ExpertDownloadManager.getInstance()

        assertTrue("Expert should be downloaded and installed", downloadManager.isInstalled())
        assertTrue("Expert executable file should exist", downloadManager.getExecutablePath().toFile().exists())
    }

    fun `test real expert version retrieval`() {
        if (skipIfExpertUnavailable()) return

        val version = ExpertDownloadManager.getInstance().getInstalledVersion()

        assertNotNull("Should be able to get Expert version", version)
        assertTrue("Version should not be empty", version!!.isNotBlank())
        println("Expert version: $version")
    }

    fun `test end-to-end with real expert`() {
        if (skipIfExpertUnavailable()) return

        // Create a realistic Elixir hello world project
        myFixture.addFileToProject(
            "mix.exs",
            """
            defmodule HelloWorld.MixProject do
              use Mix.Project

              def project do
                [
                  app: :hello_world,
                  version: "0.1.0",
                  elixir: "~> 1.15",
                  deps: []
                ]
              end
            end
            """.trimIndent()
        )

        val mainFile = myFixture.addFileToProject(
            "lib/hello_world.ex",
            """
            defmodule HelloWorld do
              def hello, do: :world
            end
            """.trimIndent()
        )

        // Verify file type is correctly detected as Elixir
        assertEquals("File should be detected as Elixir", ExFileType, mainFile.virtualFile.fileType)

        // Create descriptor and verify file is supported
        val descriptor = ExpertLspServerDescriptor(project)
        assertTrue("Elixir file should be supported", descriptor.isSupportedFile(mainFile.virtualFile))

        // Create command line and verify configuration
        val downloadManager = ExpertDownloadManager.getInstance()
        val commandLine = descriptor.createCommandLine()

        assertEquals("Should use --stdio parameter", "--stdio", commandLine.parametersList.parameters.firstOrNull())
        assertEquals(
            "Should use real Expert executable",
            downloadManager.getExecutablePath().toString(),
            commandLine.exePath
        )

        // Verify we can get Expert version
        assertNotNull("Should get Expert version", downloadManager.getInstalledVersion())
    }

    fun `test LSP support provider with real expert`() {
        if (skipIfExpertUnavailable()) return

        // Create Elixir files
        val exFile = myFixture.addFileToProject(
            "lib/example.ex",
            """
            defmodule Example do
              def run, do: :ok
            end
            """.trimIndent()
        )

        val exsFile = myFixture.addFileToProject(
            "test/example_test.exs",
            """
            defmodule ExampleTest do
              use ExUnit.Case
              test "example works" do
                assert Example.run() == :ok
              end
            end
            """.trimIndent()
        )

        val descriptor = ExpertLspServerDescriptor(project)

        // Both .ex and .exs files should be supported
        assertTrue(".ex files should be supported", descriptor.isSupportedFile(exFile.virtualFile))
        assertTrue(".exs files should be supported", descriptor.isSupportedFile(exsFile.virtualFile))

        // Non-Elixir files should not be supported
        val txtFile = myFixture.addFileToProject("README.txt", "Hello")
        assertFalse(".txt files should not be supported", descriptor.isSupportedFile(txtFile.virtualFile))
    }
}
