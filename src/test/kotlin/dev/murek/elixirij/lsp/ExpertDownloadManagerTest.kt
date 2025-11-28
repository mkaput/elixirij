package dev.murek.elixirij.lsp

import com.intellij.openapi.components.service
import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Unit tests for ExpertDownloadManager.
 *
 * Tests basic service functionality without network access.
 */
class ExpertDownloadManagerTest : BasePlatformTestCase() {

    fun `test directory path contains elixirij and expert`() {
        val expertDir = service<ExpertDownloadManager>().getDirectory()

        assertTrue(
            "Expert directory path should contain 'elixirij'",
            expertDir.toString().contains("elixirij")
        )
        assertTrue(
            "Expert directory path should contain 'expert'",
            expertDir.toString().contains("expert")
        )
    }

    fun `test executable path ends with expert`() {
        val executablePath = service<ExpertDownloadManager>().getExecutablePath()
        val expectedEnding = if (SystemInfo.isWindows) "expert.exe" else "expert"

        assertTrue(
            "Executable path should end with '$expectedEnding'",
            executablePath.toString().endsWith(expectedEnding)
        )
    }

    fun `test executable path returns non-null for current platform`() {
        // Tests that getExecutablePath() returns a valid path on the current platform
        val manager = service<ExpertDownloadManager>()
        val executablePath = manager.getExecutablePath()

        assertNotNull("Executable path should not be null", executablePath)
        assertTrue(
            "Executable path should be within plugin directory",
            executablePath.toString().contains("elixirij")
        )
    }
}
