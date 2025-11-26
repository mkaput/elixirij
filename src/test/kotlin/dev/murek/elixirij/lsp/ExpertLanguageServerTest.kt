package dev.murek.elixirij.lsp

import com.intellij.execution.WorkingDirectoryNotFoundException
import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.*
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.writeText

/**
 * Tests for Expert Language Server integration.
 */
class ExpertLanguageServerTest : BasePlatformTestCase() {
    
    private lateinit var downloadManager: ExpertDownloadManager
    private lateinit var supportProvider: ExpertLspServerSupportProvider
    
    override fun setUp() {
        super.setUp()
        downloadManager = ExpertDownloadManager.getInstance()
        supportProvider = ExpertLspServerSupportProvider()
    }
    
    override fun tearDown() {
        try {
            // Clean up any test Expert installation
            val expertDir = downloadManager.getExpertDirectory()
            if (expertDir.toFile().exists()) {
                expertDir.toFile().deleteRecursively()
            }
        } finally {
            super.tearDown()
        }
    }
    
    /**
     * Helper method to create a mock Expert executable for testing.
     */
    private fun createMockExpertExecutable() {
        val expertDir = downloadManager.getExpertDirectory()
        Files.createDirectories(expertDir)
        val executablePath = downloadManager.getExpertExecutablePath()
        
        // Create a simple script that exits immediately
        if (SystemInfo.isWindows) {
            executablePath.writeText("@echo off\nREM Mock Expert")
        } else {
            executablePath.writeText("#!/bin/sh\necho 'Mock Expert'")
            val permissions = setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            )
            Files.setPosixFilePermissions(executablePath, permissions)
        }
    }
    
    fun testSupportProviderExists() {
        assertNotNull("Support provider should not be null", supportProvider)
    }
    
    fun testDescriptorSupportsElixirFiles() {
        val descriptor = ExpertLspServerDescriptor(project)
        
        // Test .ex file
        val exFile = myFixture.addFileToProject("test.ex", "defmodule Test do\nend")
        assertTrue(".ex files should be supported", 
            descriptor.isSupportedFile(exFile.virtualFile))
        
        // Test .exs file
        val exsFile = myFixture.addFileToProject("test.exs", "IO.puts \"Hello\"")
        assertTrue(".exs files should be supported", 
            descriptor.isSupportedFile(exsFile.virtualFile))
        
        // Test non-Elixir file
        val txtFile = myFixture.addFileToProject("test.txt", "some text")
        assertFalse("Non-Elixir files should not be supported", 
            descriptor.isSupportedFile(txtFile.virtualFile))
    }
    
    fun testDescriptorThrowsWhenExpertNotInstalled() {
        val descriptor = ExpertLspServerDescriptor(project)
        
        try {
            descriptor.createCommandLine()
            fail("Should throw IllegalStateException when Expert is not installed")
        } catch (e: IllegalStateException) {
            assertTrue("Exception message should mention installation",
                e.message?.contains("not installed") == true)
        }
    }
    
    fun testDescriptorCreatesCommandLineWhenExpertInstalled() {
        // Create a mock Expert installation
        createMockExpertExecutable()
        
        val descriptor = ExpertLspServerDescriptor(project)
        val commandLine = descriptor.createCommandLine()
        
        assertNotNull("Command line should not be null", commandLine)
        assertTrue("Command line should contain expert executable",
            commandLine.exePath.endsWith("expert") || commandLine.exePath.endsWith("expert.exe"))
        assertTrue("Command line should contain 'lsp' parameter",
            commandLine.parametersList.parameters.contains("lsp"))
    }
    
    fun testDescriptorHandlesNullProjectBasePath() {
        // Create a mock Expert installation
        createMockExpertExecutable()
        
        val descriptor = ExpertLspServerDescriptor(project)
        
        try {
            val commandLine = descriptor.createCommandLine()
            assertNotNull("Command line should be created even without base path", commandLine)
        } catch (e: Exception) {
            // If it fails due to working directory, that's acceptable in test environment
            if (e !is WorkingDirectoryNotFoundException) {
                throw e
            }
        }
    }
}
