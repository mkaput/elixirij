package dev.murek.elixirij.lsp

import dev.murek.elixirij.ExSettings
import dev.murek.elixirij.ExpertMode
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.time.Duration.Companion.seconds

class ExpertTest : BaseLspServiceTestCase() {
    override val lspServer get() = Expert.getInstance(project)

    override fun setUp() {
        super.setUp()
        ExSettings.getInstance(project).expertMode = ExpertMode.AUTOMATIC
    }

    fun `test server verification returns server info`() {
        // Ensure project base path exists (it may have been cleaned up between tests)
        project.basePath?.let { Files.createDirectories(Path.of(it)) }

        lspServer.ensureFresh()
        val descriptor = lspServer.getDescriptor()
        val result = runBlocking {
            verifyLspServer(descriptor, timeout = 15.seconds)
        }
        val serverInfo = result.getOrThrow()
        println("Server info: $serverInfo")
    }
}
