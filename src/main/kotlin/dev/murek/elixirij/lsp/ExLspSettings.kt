package dev.murek.elixirij.lsp

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

enum class CodeIntelligenceService { EXPERT, NONE, ELIXIR_LS }

enum class ExpertMode { AUTOMATIC, CUSTOM }

enum class ElixirLSMode { AUTOMATIC, CUSTOM }

@Service(Service.Level.PROJECT)
@State(name = "ExLspServerSettings")
class ExLspSettings : SimplePersistentStateComponent<ExLspSettings.State>(State()) {

    class State : BaseState() {
        var codeIntelligenceService by enum(CodeIntelligenceService.EXPERT)
        var expertMode by enum(ExpertMode.AUTOMATIC)
        var expertCustomExecutablePath by string()
        var elixirLSMode by enum(ElixirLSMode.CUSTOM)
        var elixirLSCustomExecutablePath by string()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ExLspSettings = project.service()
    }

    var codeIntelligenceService: CodeIntelligenceService
        get() = state.codeIntelligenceService
        set(value) {
            state.codeIntelligenceService = value
        }

    var expertMode: ExpertMode
        get() = state.expertMode
        set(value) {
            state.expertMode = value
        }

    var expertCustomExecutablePath: String?
        get() = state.expertCustomExecutablePath
        set(value) {
            state.expertCustomExecutablePath = value?.ifBlank { null }
        }

    var elixirLSMode: ElixirLSMode
        get() = state.elixirLSMode
        set(value) {
            state.elixirLSMode = value
        }

    var elixirLSCustomExecutablePath: String?
        get() = state.elixirLSCustomExecutablePath
        set(value) {
            state.elixirLSCustomExecutablePath = value?.ifBlank { null }
        }
}
