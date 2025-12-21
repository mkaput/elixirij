package dev.murek.elixirij.lsp

import dev.murek.elixirij.ElixirLSMode
import dev.murek.elixirij.ExSettings

class ElixirLSTest : BaseLspServiceTestCase() {
    override val lspServer get() = ElixirLS.getInstance(project)

    override fun setUp() {
        super.setUp()
        ExSettings.getInstance(project).elixirLSMode = ElixirLSMode.AUTOMATIC
    }
}
