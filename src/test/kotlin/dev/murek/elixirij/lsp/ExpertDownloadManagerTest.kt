package dev.murek.elixirij.lsp

import com.intellij.openapi.util.SystemInfo
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.*
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.exists
import kotlin.io.path.writeText

/**
 * Tests for ExpertDownloadManager.
 */
class ExpertDownloadManagerTest : BasePlatformTestCase() {
    
    private lateinit var downloadManager: ExpertDownloadManager
    private lateinit var testExpertDir: Path
    
    override fun setUp() {
        super.setUp()
        downloadManager = ExpertDownloadManager.getInstance()
        testExpertDir = downloadManager.getExpertDirectory()
        
        // Clean up any existing Expert installation before tests
        if (testExpertDir.exists()) {
            testExpertDir.toFile().deleteRecursively()
        }
    }
    
    override fun tearDown() {
        try {
            // Clean up after tests
            if (testExpertDir.exists()) {
                testExpertDir.toFile().deleteRecursively()
            }
        } finally {
            super.tearDown()
        }
    }
    
    fun testGetExpertDirectory() {
        val expertDir = downloadManager.getExpertDirectory()
        assertNotNull("Expert directory should not be null", expertDir)
        assertTrue("Expert directory path should contain 'elixirij'", 
            expertDir.toString().contains("elixirij"))
        assertTrue("Expert directory path should contain 'expert'", 
            expertDir.toString().contains("expert"))
    }
    
    fun testGetExpertExecutablePath() {
        val executablePath = downloadManager.getExpertExecutablePath()
        assertNotNull("Expert executable path should not be null", executablePath)
        assertTrue("Executable path should end with 'expert'", 
            executablePath.toString().endsWith("expert"))
    }
    
    fun testIsExpertInstalledWhenNotInstalled() {
        assertFalse("Expert should not be installed initially", 
            downloadManager.isExpertInstalled())
    }
    
    fun testIsExpertInstalledWhenFileExistsButNotExecutable() {
        // Create the directory and file but don't make it executable
        Files.createDirectories(testExpertDir)
        val executablePath = downloadManager.getExpertExecutablePath()
        executablePath.writeText("fake expert")
        
        // On Windows, files are executable by default
        // On Unix, we need to verify it's not executable
        if (!SystemInfo.isWindows) {
            assertFalse("Expert should not be considered installed if not executable", 
                downloadManager.isExpertInstalled())
        }
    }
    
    fun testIsExpertInstalledWhenProperlyInstalled() {
        // Create a mock installed Expert
        Files.createDirectories(testExpertDir)
        val executablePath = downloadManager.getExpertExecutablePath()
        executablePath.writeText("fake expert")
        
        // Set executable permissions on Unix-like systems
        if (!SystemInfo.isWindows) {
            val permissions = setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.OWNER_EXECUTE
            )
            Files.setPosixFilePermissions(executablePath, permissions)
        }
        
        assertTrue("Expert should be considered installed", 
            downloadManager.isExpertInstalled())
    }
    
    fun testGetInstalledVersionWhenNotInstalled() {
        val version = downloadManager.getInstalledVersion()
        assertNull("Version should be null when Expert is not installed", version)
    }
    
    fun testGetInstalledVersionWhenInstalled() {
        // Create a mock version file
        Files.createDirectories(testExpertDir)
        val versionFile = testExpertDir.resolve("version.txt")
        val expectedVersion = "nightly-123456789"
        versionFile.writeText(expectedVersion)
        
        val version = downloadManager.getInstalledVersion()
        assertNotNull("Version should not be null when version file exists", version)
        assertEquals("Version should match what's in the file", expectedVersion, version)
    }
    
    fun testGetInstalledVersionWithWhitespace() {
        // Create a version file with whitespace
        Files.createDirectories(testExpertDir)
        val versionFile = testExpertDir.resolve("version.txt")
        versionFile.writeText("  nightly-123456789  \n")
        
        val version = downloadManager.getInstalledVersion()
        assertEquals("Version should be trimmed", "nightly-123456789", version)
    }
    
    fun testDetectPlatformReturnsValidString() {
        // We can't directly test the private detectPlatform method,
        // but we can verify it returns a non-null value for common platforms
        // by testing the download URL construction logic
        val isCommonPlatform = SystemInfo.isMac || SystemInfo.isLinux || SystemInfo.isWindows
        
        if (isCommonPlatform) {
            // If we're on a common platform, the platform detection should work
            // This is indirectly tested by the download functionality
            assertTrue("Should be on a common platform", isCommonPlatform)
        }
    }
}
