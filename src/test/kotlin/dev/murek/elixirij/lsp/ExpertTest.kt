package dev.murek.elixirij.lsp

import dev.murek.elixirij.ExSettings
import dev.murek.elixirij.ExpertMode

class ExpertTest : BaseLspServiceTestCase() {
    override val lspServer get() = Expert.getInstance(project)

    override fun setUp() {
        super.setUp()
        ExSettings.getInstance(project).expertMode = ExpertMode.AUTOMATIC
    }
}
