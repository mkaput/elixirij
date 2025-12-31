package dev.murek.elixirij.toolchain

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.PathEnvironmentVariableUtil
import com.intellij.execution.process.ScriptRunnerUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.validation.DialogValidation
import com.intellij.openapi.ui.validation.validationPathErrorFor
import com.intellij.openapi.util.SystemInfo
import dev.murek.elixirij.ExBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isExecutable
import kotlin.io.path.nameWithoutExtension

@ConsistentCopyVisibility
data class ExToolchain private constructor(
    val elixir: Path,
    val elixirc: Path,
    val mix: Path,
    val iex: Path,
) {
    /**
     * Validates toolchain paths.
     * @return null if custom dialog data is correct.
     */
    fun validate(): String? {
        val missing = mutableListOf<String>()

        if (!elixir.exists() || !elixir.isExecutable()) missing.add("elixir")
        if (!elixirc.exists() || !elixirc.isExecutable()) missing.add("elixirc")
        if (!mix.exists() || !mix.isExecutable()) missing.add("mix")
        if (!iex.exists() || !iex.isExecutable()) missing.add("iex")

        return when {
            missing.isEmpty() -> null
            missing.size == 1 -> ExBundle.message("toolchain.validation.missingExecutable", missing[0])
            else -> ExBundle.message("toolchain.validation.missingExecutables", missing.joinToString(", "))
        }
    }

    /**
     * Throws [IllegalStateException] if the toolchain is invalid.
     */
    fun checkValid() = validate()?.let { throw IllegalStateException(it) }

    /**
     * Fetches the Elixir version by running `elixir --version`.
     *
     * Returns `null` if the version cannot be determined.
     * Example output: `"Elixir 1.17.0 (compiled with Erlang/OTP 26)"`
     */
    suspend fun fetchVersion(project: Project): String? = withContext(Dispatchers.IO) {
        val commandLine = GeneralCommandLine("$elixir", "--version").withWorkDirectory(project.basePath)
        val output = ScriptRunnerUtil.getProcessOutput(commandLine)

        // Parse: "Elixir 1.17.0 (compiled with Erlang/OTP 26)"
        val match = Regex("""Elixir (\d+\.\d+\.\d+)""").find(output)
        match?.groupValues?.get(1)
    }

    companion object {
        fun create(elixir: Path): ExToolchain = ExToolchain(
            elixir = elixir,
            elixirc = anyOsBin(elixir.parent / "elixirc"),
            mix = anyOsBin(elixir.parent / "mix"),
            iex = anyOsBin(elixir.parent / "iex"),
        )

        private fun anyOsBin(unixFilePath: Path): Path = if (SystemInfo.isWindows) {
            PathEnvironmentVariableUtil.getWindowsExecutableFileExtensions()
                .firstNotNullOfOrNull { ext -> unixFilePath.withExtension(ext).takeIf { it.exists() } }
                ?: unixFilePath.withExtension(".bat") // This is what Elixir zip archives used to contain.
        } else {
            unixFilePath
        }

        private fun Path.withExtension(ext: String): Path = resolveSibling(nameWithoutExtension + ext)

        /**
         * Creates a toolchain from the given `elixir` executable and checks its validity.
         */
        fun createValid(elixir: Path): Result<ExToolchain> = runCatching { create(elixir).apply { checkValid() } }

        val CHECK_VALID: DialogValidation.WithParameter<() -> String> = validationPathErrorFor {
            create(it).validate()
        }
    }
}
