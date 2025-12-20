package dev.murek.elixirij.lsp

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class CodeIntelligenceServiceTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        // Reset settings to default for each test
        val settings = ExLspSettings.getInstance(project)
        settings.loadState(ExLspSettings.State())
    }

    fun `test expert is default code intelligence service`() {
        val settings = ExLspSettings.getInstance(project)
        assertEquals(CodeIntelligenceService.EXPERT, settings.codeIntelligenceService)
    }

    fun `test getConfiguredServiceInstance returns Expert by default`() {
        val service = ExLspServerService.getConfiguredServiceInstance(project)
        assertTrue(service is Expert)
    }

    fun `test getConfiguredServiceInstance returns ElixirLS when configured`() {
        val settings = ExLspSettings.getInstance(project)
        settings.codeIntelligenceService = CodeIntelligenceService.ELIXIR_LS
        val service = ExLspServerService.getConfiguredServiceInstance(project)
        assertTrue(service is ElixirLS)
    }

    fun `test getConfiguredServiceInstance returns NoCodeIntelligenceService when configured`() {
        val settings = ExLspSettings.getInstance(project)
        settings.codeIntelligenceService = CodeIntelligenceService.NONE
        val service = ExLspServerService.getConfiguredServiceInstance(project)
        assertSame(NoCodeIntelligenceService, service)
    }
}
