package dev.murek.elixirij.lsp

import com.intellij.openapi.diagnostic.Logger
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
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val LOG = Logger.getInstance("dev.murek.elixirij.lsp.LspServerVerification")

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
            val startedAt = System.currentTimeMillis()
            val stderrLines = mutableListOf<String>()
            thread(name = "expert-lsp-stderr", isDaemon = true) {
                process.errorStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        synchronized(stderrLines) {
                            if (stderrLines.size < 200) {
                                stderrLines.add(line)
                            }
                        }
                    }
                }
            }

            try {
                // Create a minimal client that does nothing (we only need to send requests)
                val client = object : LanguageClient {
                    override fun telemetryEvent(obj: Any?) {}

                    override fun publishDiagnostics(diagnostics: PublishDiagnosticsParams?) {}

                    override fun showMessage(messageParams: MessageParams?) {}

                    override fun showMessageRequest(requestParams: ShowMessageRequestParams?): CompletableFuture<MessageActionItem> =
                        CompletableFuture.completedFuture(null)

                    override fun logMessage(message: MessageParams?) {}

                    override fun registerCapability(params: RegistrationParams?): CompletableFuture<Void> =
                        CompletableFuture.completedFuture(null)

                    override fun unregisterCapability(params: UnregistrationParams?): CompletableFuture<Void> =
                        CompletableFuture.completedFuture(null)
                }

                val launcher = LSPLauncher.createClientLauncher(client, process.inputStream, process.outputStream)
                val server: LanguageServer = launcher.remoteProxy

                // Start listening for messages in a background thread
                val listening = launcher.startListening()

                // Send initialize request
                val initializeParams = descriptor.createInitializeParams()
                val rootDir = commandLine.workDirectory?.toPath()
                    ?: Files.createTempDirectory("elixirij-lsp-verify")
                Files.createDirectories(rootDir)
                val workspaceFolder = workspaceFolderFor(rootDir)
                initializeParams.workspaceFolders = listOf(workspaceFolder)
                LOG.info(
                    "Verifying LSP server: cmd='${commandLine.commandLineString}' " +
                        "workDir='${commandLine.workDirectory}' rootUri='${workspaceFolder.uri}'"
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
                if (!process.isAlive) {
                    LOG.warn("LSP process exited early with code=${process.exitValue()}")
                }
                // Ensure the process is terminated
                if (process.isAlive) {
                    process.destroyForcibly()
                }
                process.awaitExit()
                val elapsedMs = System.currentTimeMillis() - startedAt
                if (stderrLines.isNotEmpty()) {
                    val snapshot = synchronized(stderrLines) { stderrLines.toList() }
                    LOG.warn("LSP stderr (${snapshot.size} lines, ${elapsedMs}ms):\n${snapshot.joinToString("\n")}")
                } else {
                    LOG.info("LSP verification completed in ${elapsedMs}ms (no stderr output).")
                }
            }
        }
    }
}

private fun workspaceFolderFor(rootDir: Path): WorkspaceFolder {
    val rootUri = rootDir.toUri().toString()
    val name = rootDir.fileName?.toString() ?: "workspace"
    return WorkspaceFolder(rootUri, name)
}
