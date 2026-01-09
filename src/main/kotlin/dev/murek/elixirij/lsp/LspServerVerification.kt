package dev.murek.elixirij.lsp

import com.intellij.platform.lsp.api.LspServerDescriptor
import com.intellij.util.io.awaitExit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient
import org.eclipse.lsp4j.services.LanguageServer
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Verifies an LSP server by launching it, performing initialization handshake,
 * and extracting server info (name and version).
 *
 * This function starts the server process, sends the `initialize` request,
 * reads the `InitializeResult`, sends the `initialized` notification,
 * and then shuts down the server gracefully.
 *
 * @param descriptor The LSP server descriptor that knows how to start the server
 * @param timeout Maximum time to wait for the initialization to complete
 * @return Result containing ServerInfo on success, or an error on failure
 */
suspend fun verifyLspServer(
    descriptor: LspServerDescriptor, timeout: Duration = 30.seconds
): Result<ServerInfo> = runCatching {
    withTimeout(timeout) {
        withContext(Dispatchers.IO) {
            val commandLine = descriptor.createCommandLine().withCharset(Charsets.UTF_8)
            val process = commandLine.createProcess()

            try {
                // Create a minimal client that does nothing (we only need to send requests)
                val client = object : LanguageClient {
                    override fun telemetryEvent(obj: Any?) {}

                    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {}

                    override fun showMessage(messageParams: MessageParams?) {}

                    override fun showMessageRequest(requestParams: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> =
                        CompletableFuture.completedFuture(null)

                    override fun logMessage(message: MessageParams?) {}
                }

                val launcher = LSPLauncher.createClientLauncher(client, process.inputStream, process.outputStream)
                val server: LanguageServer = launcher.remoteProxy

                // Start listening for messages in a background thread
                val listening = launcher.startListening()

                // Send initialize request
                val initializeParams = descriptor.createInitializeParams()
                val existingRootUri = initializeParams.rootUri?.takeIf { it.isNotBlank() }
                val uriRootDir = existingRootUri?.let { uri ->
                    runCatching { Path.of(URI(uri)) }.getOrNull()
                }
                val rootDir = commandLine.workDirectory?.toPath()
                    ?: uriRootDir
                    ?: Files.createTempDirectory("elixirij-lsp-verify")
                Files.createDirectories(rootDir)
                val systemIndependentPath = rootDir.toAbsolutePath().toString().replace('\\', '/')
                val rootUri = if (systemIndependentPath.startsWith('/')) {
                    "file://$systemIndependentPath"
                } else {
                    "file:///$systemIndependentPath"
                }
                initializeParams.rootUri = rootUri
                initializeParams.rootPath = rootDir.toString()
                initializeParams.workspaceFolders = listOf(
                    WorkspaceFolder(rootUri, rootDir.fileName?.toString() ?: "workspace")
                )
                val initializeResult = server.initialize(initializeParams).await()

                // Send initialized notification
                server.initialized(InitializedParams())

                // Extract server info
                val serverInfo = checkNotNull(initializeResult.serverInfo) {
                    "Server did not provide serverInfo in InitializeResult"
                }

                // Shutdown the server gracefully
                try {
                    server.shutdown().await()
                    server.exit()
                } catch (_: Exception) {
                    // Ignore shutdown errors - we got what we needed
                }

                // Cancel the listening future to stop the reader thread
                listening.cancel(true)

                serverInfo
            } finally {
                // Ensure the process is terminated
                if (process.isAlive) {
                    process.destroyForcibly()
                }
                process.awaitExit()
            }
        }
    }
}
