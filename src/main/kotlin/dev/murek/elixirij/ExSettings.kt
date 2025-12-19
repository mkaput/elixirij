package dev.murek.elixirij

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

enum class CodeIntelligenceService { EXPERT, NONE }

enum class ExpertMode { AUTOMATIC, CUSTOM }

@Service(Service.Level.PROJECT)
@State(name = "Elixir", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class ExSettings : SimplePersistentStateComponent<ExSettings.State>(State()) {

    class State : BaseState() {
        var codeIntelligenceService by enum(CodeIntelligenceService.EXPERT)
        var expertMode by enum(ExpertMode.AUTOMATIC)
        var expertCustomExecutablePath by string()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ExSettings = project.service()
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
}
