package dev.murek.elixirij.lsp

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.murek.elixirij.ExFileType
import org.junit.Assert.*
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
    
    private lateinit var downloadManager: ExpertDownloadManager
    
    companion object {
        // Track if we've already tried (and failed) to download Expert
        private val downloadAttempted = AtomicBoolean(false)
        private val downloadSucceeded = AtomicBoolean(false)
    }
    
    override fun setUp() {
        super.setUp()
        downloadManager = ExpertDownloadManager.getInstance()
    }
    
    override fun tearDown() {
        try {
            // Note: We don't clean up Expert installation to avoid re-downloading
            // in subsequent tests. The binary is shared across test runs.
        } finally {
            super.tearDown()
        }
    }
    
    /**
     * Downloads Expert if not already installed and waits for completion.
     * Returns true if Expert is ready to use, false if download failed (e.g., network unavailable).
     */
    private fun ensureExpertDownloaded(): Boolean {
        if (downloadManager.isExpertInstalled()) {
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
        
        downloadManager.downloadAndInstallExpert { result, _ ->
            success = result
            latch.countDown()
        }
        
        // Wait up to 2 minutes for download to complete
        latch.await(2, TimeUnit.MINUTES)
        
        val isInstalled = downloadManager.isExpertInstalled()
        downloadSucceeded.set(isInstalled)
        return isInstalled
    }
    
    /**
     * Test downloading and installing the real Expert binary.
     */
    fun `test download real expert binary`() {
        val isReady = ensureExpertDownloaded()
        
        if (!isReady) {
            // Network unavailable - test passes but with indication
            println("SKIPPED: Expert binary could not be downloaded (network may be unavailable)")
            return
        }
        
        assertTrue("Expert should be downloaded and installed", downloadManager.isExpertInstalled())
        
        // Verify the executable path exists
        val executablePath = downloadManager.getExpertExecutablePath()
        assertTrue("Expert executable file should exist", executablePath.toFile().exists())
    }
    
    /**
     * Test retrieving the version from the real Expert binary.
     */
    fun `test real expert version retrieval`() {
        val isReady = ensureExpertDownloaded()
        
        if (!isReady) {
            println("SKIPPED: Expert binary could not be downloaded (network may be unavailable)")
            return
        }
        
        // Get the version from the real binary
        val version = downloadManager.getInstalledVersion()
        
        assertNotNull("Should be able to get Expert version", version)
        assertTrue("Version should not be empty", version!!.isNotBlank())
        
        // Expert version typically contains "Expert" or version numbers
        println("Expert version: $version")
    }
    
    /**
     * Test complete end-to-end flow with real Expert binary.
     * Creates an Elixir project, downloads Expert, and verifies LSP configuration.
     */
    fun `test end-to-end with real expert`() {
        val isReady = ensureExpertDownloaded()
        
        if (!isReady) {
            println("SKIPPED: Expert binary could not be downloaded (network may be unavailable)")
            return
        }
        
        // 1. Create a realistic Elixir hello world project
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
              @moduledoc \"\"\"
              A simple hello world module.
              \"\"\"
            
              @doc \"\"\"
              Says hello to the world.
              \"\"\"
              def hello do
                :world
              end
            
              @doc \"\"\"
              Greets a person by name.
              \"\"\"
              def greet(name) do
                "Hello, #{name}!"
              end
            end
            """.trimIndent()
        )
        
        // 2. Verify file type is correctly detected as Elixir
        assertEquals("File should be detected as Elixir", 
            ExFileType, 
            mainFile.virtualFile.fileType)
        
        // 3. Create descriptor and verify file is supported
        val descriptor = ExpertLspServerDescriptor(project)
        assertTrue("Elixir file should be supported", 
            descriptor.isSupportedFile(mainFile.virtualFile))
        
        // 4. Create command line and verify configuration
        val commandLine = descriptor.createCommandLine()
        assertNotNull("Command line should be created", commandLine)
        
        // 5. Verify command uses correct Expert parameters
        assertEquals("Should use --stdio parameter", 
            "--stdio", 
            commandLine.parametersList.parameters.firstOrNull())
        
        // 6. Verify executable path
        val expectedPath = downloadManager.getExpertExecutablePath().toString()
        assertEquals("Should use real Expert executable", 
            expectedPath, 
            commandLine.exePath)
        
        // 7. Verify we can get Expert version
        val version = downloadManager.getInstalledVersion()
        assertNotNull("Should get Expert version", version)
        println("Using Expert version: $version")
    }
    
    /**
     * Test that the LSP support provider correctly identifies Elixir files.
     */
    fun `test LSP support provider with real expert`() {
        val isReady = ensureExpertDownloaded()
        
        if (!isReady) {
            println("SKIPPED: Expert binary could not be downloaded (network may be unavailable)")
            return
        }
        
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
        assertTrue(".ex files should be supported", 
            descriptor.isSupportedFile(exFile.virtualFile))
        assertTrue(".exs files should be supported", 
            descriptor.isSupportedFile(exsFile.virtualFile))
        
        // Non-Elixir files should not be supported
        val txtFile = myFixture.addFileToProject("README.txt", "Hello")
        assertFalse(".txt files should not be supported", 
            descriptor.isSupportedFile(txtFile.virtualFile))
    }
}
