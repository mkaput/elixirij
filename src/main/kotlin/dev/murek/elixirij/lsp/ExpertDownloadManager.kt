package dev.murek.elixirij.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.util.ExecUtil
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.HttpRequests
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.exists

private val LOG = logger<ExpertDownloadManager>()

private const val EXPERT_NIGHTLY_BASE_URL = "https://github.com/elixir-lang/expert/releases/download/nightly"
private const val EXPERT_DIR_NAME = "expert"
private const val EXPERT_EXECUTABLE_NAME = "expert"
private const val UPDATE_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000 // 24 hours in milliseconds

/**
 * Service responsible for downloading and managing Expert language server nightly builds.
 */
@Service(Service.Level.APP)
class ExpertDownloadManager {
    
    companion object {
        @JvmStatic
        fun getInstance(): ExpertDownloadManager = service()
    }
    
    /**
     * Get the directory where Expert is stored.
     * Uses IntelliJ's plugin-specific directory for proper isolation.
     */
    fun getDirectory(): Path {
        val pluginDir = PathManager.getPluginsPath()
        return Path.of(pluginDir, "elixirij", EXPERT_DIR_NAME)
    }
    
    /**
     * Get the path to the Expert executable.
     */
    fun getExecutablePath(): Path {
        return getDirectory().resolve(EXPERT_EXECUTABLE_NAME)
    }
    
    /**
     * Check if Expert is installed.
     */
    fun isInstalled(): Boolean {
        val executablePath = getExecutablePath()
        return executablePath.exists() && Files.isExecutable(executablePath)
    }
    
    /**
     * Get the currently installed version of Expert by running the CLI.
     * Returns null if not installed or version cannot be determined.
     */
    fun getInstalledVersion(): String? {
        if (!isInstalled()) return null
        
        return try {
            val commandLine = GeneralCommandLine(getExecutablePath().toString(), "--version")
            val output = ExecUtil.execAndGetOutput(commandLine)
            if (output.exitCode == 0) output.stdout.trim() else null
        } catch (e: Exception) {
            LOG.warn("Failed to get Expert version", e)
            null
        }
    }
    
    /**
     * Download and install the latest nightly build of Expert.
     */
    fun downloadAndInstall(onComplete: (Boolean, String?) -> Unit) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(
            null,
            "Downloading Expert Language Server",
            true
        ) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = "Determining platform..."
                    val platform = detectPlatform()
                    
                    if (platform == null) {
                        onComplete(false, "Unsupported platform: ${SystemInfo.OS_NAME} ${SystemInfo.OS_ARCH}")
                        return
                    }
                    
                    indicator.text = "Downloading Expert nightly build..."
                    indicator.isIndeterminate = false
                    
                    val expertDir = getDirectory()
                    Files.createDirectories(expertDir)
                    
                    val downloadUrl = "$EXPERT_NIGHTLY_BASE_URL/expert-$platform"
                    val tempFile = Files.createTempFile("expert", ".tmp")
                    
                    try {
                        downloadFile(downloadUrl, tempFile, indicator)
                        
                        indicator.text = "Installing Expert..."
                        indicator.fraction = 0.9
                        
                        val executablePath = getExecutablePath()
                        Files.move(tempFile, executablePath, StandardCopyOption.REPLACE_EXISTING)
                        
                        // Set executable permissions on Unix-like systems
                        if (!SystemInfo.isWindows) {
                            val permissions = setOf(
                                PosixFilePermission.OWNER_READ,
                                PosixFilePermission.OWNER_WRITE,
                                PosixFilePermission.OWNER_EXECUTE,
                                PosixFilePermission.GROUP_READ,
                                PosixFilePermission.GROUP_EXECUTE,
                                PosixFilePermission.OTHERS_READ,
                                PosixFilePermission.OTHERS_EXECUTE
                            )
                            Files.setPosixFilePermissions(executablePath, permissions)
                        }
                        
                        indicator.fraction = 1.0
                        onComplete(true, null)
                    } finally {
                        if (tempFile.exists()) {
                            Files.deleteIfExists(tempFile)
                        }
                    }
                } catch (e: Exception) {
                    LOG.warn("Failed to download Expert (network may be unavailable)", e)
                    onComplete(false, "Failed to download Expert: ${e.message}")
                }
            }
        })
    }
    
    /**
     * Check if an update is available and install it if needed.
     * Uses the binary's modification time to determine if update is needed.
     */
    fun checkAndUpdate(onComplete: (Boolean, String?) -> Unit) {
        val shouldUpdate = if (!isInstalled()) {
            true
        } else {
            val executablePath = getExecutablePath()
            val lastModified = Files.getLastModifiedTime(executablePath).toMillis()
            val updateThreshold = System.currentTimeMillis() - UPDATE_CHECK_INTERVAL_MS
            lastModified < updateThreshold
        }
        
        if (shouldUpdate) {
            downloadAndInstall(onComplete)
        } else {
            onComplete(true, null)
        }
    }
    
    /**
     * Detect the current platform and return the appropriate platform string for Expert downloads.
     */
    private fun detectPlatform(): String? {
        val arch = SystemInfo.OS_ARCH.lowercase()
        val isArm64 = SystemInfo.isAarch64
        // Check for x86_64/AMD64 but exclude ARM architectures
        val is64Bit = !isArm64 && (arch.contains("x86_64") || arch.contains("amd64") || arch.contains("x64"))
        
        return when {
            // Prioritize ARM64 detection to ensure correct platform identification
            SystemInfo.isMac && isArm64 -> "aarch64-apple-darwin"
            SystemInfo.isLinux && isArm64 -> "aarch64-unknown-linux-gnu"
            // Fall back to x86_64 for non-ARM platforms
            SystemInfo.isMac && is64Bit -> "x86_64-apple-darwin"
            SystemInfo.isLinux && is64Bit -> "x86_64-unknown-linux-gnu"
            SystemInfo.isWindows && is64Bit -> "x86_64-pc-windows-msvc.exe"
            else -> null
        }
    }
    
    /**
     * Download a file from a URL with progress tracking.
     * Uses HttpRequests default timeouts (10 seconds for connect and read).
     */
    private fun downloadFile(urlString: String, destination: Path, indicator: ProgressIndicator) {
        HttpRequests.request(urlString)
            .connect { request ->
                request.saveToFile(destination.toFile(), indicator)
            }
    }
}
