package dev.murek.elixirij.ide.formatter

import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import dev.murek.elixirij.ExBundle
import dev.murek.elixirij.lang.ExFile
import dev.murek.elixirij.mix.mix
import dev.murek.elixirij.toolchain.elixirToolchain
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.readText
import kotlin.io.path.writeText

class ExMixFormatService : AsyncDocumentFormattingService() {
    override fun canFormat(file: PsiFile): Boolean {
        if (file !is ExFile) return false
        if (file.project.elixirToolchain == null) return false
        return file.project.mix.projectRootFor(file) != null
    }

    override fun getFeatures(): Set<FormattingService.Feature> = emptySet()

    override fun getNotificationGroupId(): String = "ElixirIJ"

    override fun getName(): String = ExBundle.message("formatter.mixFormat.name")

    override fun createFormattingTask(request: AsyncFormattingRequest): FormattingTask? {
        val context = request.context
        val project = context.project
        val workingDirectory = project.mix.projectRootFor(context.containingFile) ?: return null
        return object : FormattingTask {
            private var handler: OSProcessHandler? = null
            private var tempFile: Path? = null

            override fun run() {
                val extension = resolveFormatExtension(context.containingFile)
                val formatFile = try {
                    createTempFormatFile(workingDirectory, extension, request.documentText)
                } catch (_: Exception) {
                    request.onError(getName(), ExBundle.message("formatter.mixFormat.failed"))
                    return
                }
                val commandLine = project.mix.buildMixCommandLine(
                    workingDirectory,
                    "format",
                    formatFile.toString(),
                )

                if (commandLine == null) {
                    formatFile.deleteIfExists()
                    return
                }

                val processHandler = OSProcessHandler(commandLine.withCharset(StandardCharsets.UTF_8))
                tempFile = formatFile
                handler = processHandler

                processHandler.addProcessListener(object : CapturingProcessAdapter() {
                    override fun processTerminated(event: ProcessEvent) {
                        val output = output
                        val formattedFile = tempFile
                        try {
                            if (event.exitCode == 0 && formattedFile != null) {
                                request.onTextReady(formattedFile.readText(StandardCharsets.UTF_8))
                            } else {
                                val message = output.stderr.ifBlank { output.stdout }
                                request.onError(
                                    getName(),
                                    message.ifBlank { ExBundle.message("formatter.mixFormat.failed") })
                            }
                        } finally {
                            formattedFile?.deleteIfExists()
                        }
                    }
                })

                processHandler.startNotify()
            }

            override fun cancel(): Boolean {
                handler?.destroyProcess()
                tempFile?.deleteIfExists()
                return true
            }

            override fun isRunUnderProgress(): Boolean = true
        }
    }

}

private const val DEFAULT_MIX_FORMAT_EXTENSION = "exs"

private fun resolveFormatExtension(file: PsiFile): String =
    file.fileType.defaultExtension.takeIf { it.isNotBlank() } ?: DEFAULT_MIX_FORMAT_EXTENSION

private fun createTempFormatFile(workingDirectory: Path, extension: String, content: String): Path {
    val suffix = if (extension.startsWith(".")) extension else ".$extension"
    val tempFile = FileUtil.createTempFile(
        workingDirectory.toFile(),
        "elixirij-format-",
        suffix,
        true,
        true,
    )
    tempFile.toPath().writeText(content, StandardCharsets.UTF_8)
    return tempFile.toPath()
}
