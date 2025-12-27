package dev.murek.elixirij.lsp

import dev.murek.elixirij.ElixirLSMode
import dev.murek.elixirij.ExSettings

/**
 * Tests for ElixirLS language server service.
 *
 * Note: Unlike [ExpertTest], this class does not include a server verification test.
 * ElixirLS requires a proper Elixir project environment (with mix.exs) to initialize,
 * which is not available in the test environment. Expert is a standalone binary
 * that doesn't have this requirement.
 */
class ElixirLSTest : BaseLspServiceTestCase() {
    override val lspServer get() = ElixirLS.getInstance(project)

    override fun setUp() {
        super.setUp()
        ExSettings.getInstance(project).elixirLSMode = ElixirLSMode.AUTOMATIC
    }
}
