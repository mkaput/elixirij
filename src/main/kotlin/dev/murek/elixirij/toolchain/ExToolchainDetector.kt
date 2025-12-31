package dev.murek.elixirij.toolchain

import com.intellij.execution.configurations.PathEnvironmentVariableUtil

/**
 * Auto-detects an Elixir toolchain.
 *
 * The resulting toolchain is guaranteed to be valid.
 */
fun detectElixirToolchain(): ExToolchain? =
    PathEnvironmentVariableUtil.findExecutableInPathOnAnyOS("elixir")?.toPath()
        ?.let { ExToolchain.createValid(it).getOrNull() }
