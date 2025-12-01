package dev.murek.elixirij.lsp

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "ExpertSettings")
class ExpertSettings(private val project: Project) : SimplePersistentStateComponent<ExpertSettings.State>(State()) {

    class State : BaseState() {
        var mode by enum(ExpertMode.AUTOMATIC)
        var customExecutablePath by string()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ExpertSettings = project.service()
    }

    var mode: ExpertMode
        get() = state.mode
        set(value) {
            state.mode = value
        }

    var customExecutablePath: String?
        get() = state.customExecutablePath
        set(value) {
            state.customExecutablePath = value?.ifBlank { null }
        }

    /**
     * Notify all listeners that settings have changed.
     *
     * This should be called after settings are modified (e.g., in [ExpertConfigurable.apply]).
     */
    fun fireSettingsChanged() {
        project.messageBus
            .syncPublisher(ExpertSettingsListener.TOPIC)
            .settingsChanged()
    }
}
