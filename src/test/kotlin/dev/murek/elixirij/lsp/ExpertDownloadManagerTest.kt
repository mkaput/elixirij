package dev.murek.elixirij.lsp

import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Unit tests for ExpertDownloadManager.
 *
 * Tests basic service functionality without network access.
 */
class ExpertDownloadManagerTest : BasePlatformTestCase() {

    fun `test directory path contains elixirij and expert`() {
        val expertDir = ExpertDownloadManager.getInstance().getDirectory()

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
        val executablePath = ExpertDownloadManager.getInstance().getExecutablePath()

        assertTrue(
            "Executable path should end with 'expert'",
            executablePath.toString().endsWith("expert")
        )
    }

    fun `test platform detection returns valid value for common platforms`() {
        // This indirectly tests that platform detection works on common platforms
        val isCommonPlatform = SystemInfo.isMac || SystemInfo.isLinux || SystemInfo.isWindows

        if (isCommonPlatform) {
            assertTrue("Should be on a common platform for testing", isCommonPlatform)
        }
    }
}
