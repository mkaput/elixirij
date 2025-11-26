package dev.murek.elixirij.lsp

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.LanguageServerFactory
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider
import org.eclipse.lsp4j.services.LanguageServer
import java.io.InputStream
import java.io.OutputStream

/**
 * Factory for creating Expert Language Server instances.
 */
class ExpertLanguageServerFactory : LanguageServerFactory {
    override fun createConnectionProvider(project: Project): StreamConnectionProvider {
        return ExpertStreamConnectionProvider(project)
    }
    
    override fun createLanguageClient(project: Project): LanguageClientImpl {
        return LanguageClientImpl(project)
    }
    
    override fun getServerInterface(): Class<out LanguageServer> {
        return LanguageServer::class.java
    }
}

/**
 * Stream connection provider for Expert Language Server.
 */
class ExpertStreamConnectionProvider(private val project: Project) : StreamConnectionProvider {
    private var process: Process? = null
    
    override fun start() {
        val downloadManager = ExpertDownloadManager.getInstance()
        
        // Ensure Expert is installed
        if (!downloadManager.isExpertInstalled()) {
            throw IllegalStateException("Expert language server is not installed. Please install it from the settings.")
        }
        
        val expertPath = downloadManager.getExpertExecutablePath()
        
        val commandLine = GeneralCommandLine()
        commandLine.exePath = expertPath.toString()
        commandLine.addParameter("lsp")
        
        // Set working directory if available
        val basePath = project.basePath
        if (basePath != null) {
            commandLine.withWorkDirectory(basePath)
        }
        
        process = commandLine.createProcess()
    }
    
    override fun getInputStream(): InputStream? {
        return process?.inputStream
    }
    
    override fun getOutputStream(): OutputStream? {
        return process?.outputStream
    }
    
    override fun stop() {
        process?.destroy()
        process = null
    }
}

