package dev.murek.elixirij.lsp

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.util.SystemInfo
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.exists

/**
 * Service responsible for downloading and managing Expert language server nightly builds.
 */
@Service(Service.Level.APP)
class ExpertDownloadManager {
    private val logger = Logger.getInstance(ExpertDownloadManager::class.java)
    
    companion object {
        private const val EXPERT_NIGHTLY_BASE_URL = "https://github.com/elixir-lang/expert/releases/download/nightly"
        private const val EXPERT_DIR_NAME = "expert"
        private const val EXPERT_EXECUTABLE_NAME = "expert"
        private const val VERSION_FILE_NAME = "version.txt"
        private const val UPDATE_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000 // 24 hours in milliseconds
        
        fun getInstance(): ExpertDownloadManager {
            return com.intellij.openapi.components.service()
        }
    }
    
    /**
     * Get the directory where Expert is stored.
     */
    fun getExpertDirectory(): Path {
        val pluginSystemDir = PathManager.getSystemPath()
        return Path.of(pluginSystemDir, "elixirij", EXPERT_DIR_NAME)
    }
    
    /**
     * Get the path to the Expert executable.
     */
    fun getExpertExecutablePath(): Path {
        return getExpertDirectory().resolve(EXPERT_EXECUTABLE_NAME)
    }
    
    /**
     * Check if Expert is installed.
     */
    fun isExpertInstalled(): Boolean {
        val executablePath = getExpertExecutablePath()
        return executablePath.exists() && Files.isExecutable(executablePath)
    }
    
    /**
     * Get the currently installed version of Expert.
     * Returns null if not installed or version cannot be determined.
     */
    fun getInstalledVersion(): String? {
        val versionFile = getExpertDirectory().resolve(VERSION_FILE_NAME)
        return if (versionFile.exists()) {
            try {
                Files.readString(versionFile).trim()
            } catch (e: Exception) {
                logger.warn("Failed to read Expert version file", e)
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Download and install the latest nightly build of Expert.
     */
    fun downloadAndInstallExpert(onComplete: (Boolean, String?) -> Unit) {
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
                    
                    val expertDir = getExpertDirectory()
                    Files.createDirectories(expertDir)
                    
                    val downloadUrl = "$EXPERT_NIGHTLY_BASE_URL/expert-$platform"
                    val tempFile = Files.createTempFile("expert", ".tmp")
                    
                    try {
                        downloadFile(downloadUrl, tempFile, indicator)
                        
                        indicator.text = "Installing Expert..."
                        indicator.fraction = 0.9
                        
                        val executablePath = getExpertExecutablePath()
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
                        
                        // Save version information
                        val versionFile = expertDir.resolve(VERSION_FILE_NAME)
                        val timestamp = System.currentTimeMillis()
                        Files.writeString(versionFile, "nightly-$timestamp")
                        
                        indicator.fraction = 1.0
                        onComplete(true, null)
                    } finally {
                        if (tempFile.exists()) {
                            Files.deleteIfExists(tempFile)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Failed to download Expert", e)
                    onComplete(false, "Failed to download Expert: ${e.message}")
                }
            }
        })
    }
    
    /**
     * Check if an update is available and install it if needed.
     */
    fun checkAndUpdateExpert(onComplete: (Boolean, String?) -> Unit) {
        // For now, we'll update if Expert is not installed or if it's older than UPDATE_CHECK_INTERVAL_MS
        val shouldUpdate = if (!isExpertInstalled()) {
            true
        } else {
            val versionFile = getExpertDirectory().resolve(VERSION_FILE_NAME)
            if (!versionFile.exists()) {
                true
            } else {
                val lastModified = Files.getLastModifiedTime(versionFile).toMillis()
                val updateThreshold = System.currentTimeMillis() - UPDATE_CHECK_INTERVAL_MS
                lastModified < updateThreshold
            }
        }
        
        if (shouldUpdate) {
            downloadAndInstallExpert(onComplete)
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
     */
    private fun downloadFile(urlString: String, destination: Path, indicator: ProgressIndicator) {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        
        try {
            connection.connect()
            
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error code: $responseCode")
            }
            
            val contentLength = connection.contentLengthLong
            var downloadedBytes = 0L
            
            connection.inputStream.use { input ->
                FileOutputStream(destination.toFile()).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        
                        if (contentLength > 0) {
                            indicator.fraction = downloadedBytes.toDouble() / contentLength * 0.9
                        }
                        
                        if (indicator.isCanceled) {
                            throw InterruptedException("Download cancelled by user")
                        }
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
