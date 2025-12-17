package dev.murek.elixirij.lsp

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

enum class ExpertMode { DISABLED, AUTOMATIC, CUSTOM }

@Service(Service.Level.PROJECT)
@State(name = "ExLspServerSettings")
class ExLspSettings : SimplePersistentStateComponent<ExLspSettings.State>(State()) {

    class State : BaseState() {
        var expertMode by enum(ExpertMode.AUTOMATIC)
        var expertCustomExecutablePath by string()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ExLspSettings = project.service()
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
