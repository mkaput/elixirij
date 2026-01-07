package dev.murek.elixirij.ide.formatter

import com.intellij.execution.process.CapturingProcessAdapter
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.formatting.service.AsyncDocumentFormattingService
import com.intellij.formatting.service.AsyncFormattingRequest
import com.intellij.formatting.service.FormattingService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import dev.murek.elixirij.ExBundle
import dev.murek.elixirij.lang.ExFile
import dev.murek.elixirij.mix.mix
import dev.murek.elixirij.toolchain.elixirToolchain
import java.nio.charset.StandardCharsets

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
        val file = context.containingFile
        if (!request.formattingRanges.isWholeFile(file.textRange)) return null

        val workingDirectory = project.mix.projectRootFor(context.containingFile) ?: return null
        val stdinFilename = resolveStdinFilename(request)
        val commandLine = project.mix.buildMixCommandLine(
            workingDirectory,
            "format",
            "--stdin-filename",
            stdinFilename,
            "-",
        ) ?: return null
        val handler = OSProcessHandler(commandLine.withCharset(StandardCharsets.UTF_8))

        return object : FormattingTask {
            override fun run() {
                handler.addProcessListener(object : CapturingProcessAdapter() {
                    override fun processTerminated(event: ProcessEvent) {
                        val output = output
                        if (event.exitCode == 0) {
                            request.onTextReady(output.stdout)
                        } else {
                            val message = output.stderr.ifBlank { output.stdout }
                            request.onError(
                                getName(),
                                message.ifBlank { ExBundle.message("formatter.mixFormat.failed") })
                        }
                    }
                })

                handler.startNotify()
                handler.processInput.use { stdin ->
                    stdin.write(request.documentText.toByteArray(StandardCharsets.UTF_8))
                    stdin.flush()
                }
            }

            override fun cancel(): Boolean {
                handler.destroyProcess()
                return true
            }

            override fun isRunUnderProgress(): Boolean = true
        }
    }

    private fun resolveStdinFilename(request: AsyncFormattingRequest): String =
        request.context.virtualFile
            ?.takeIf { it.isInLocalFileSystem }
            ?.path
            ?: request.ioFile?.path
            ?: DEFAULT_STDIN_FILENAME

    private fun List<TextRange>.isWholeFile(fileRange: TextRange): Boolean = size == 1 && first() == fileRange

    companion object {
        private const val DEFAULT_STDIN_FILENAME = "stdin.exs"
    }
}
