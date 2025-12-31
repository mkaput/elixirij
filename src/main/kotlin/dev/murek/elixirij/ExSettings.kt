package dev.murek.elixirij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import kotlinx.coroutines.CoroutineScope

enum class CodeIntelligenceService { EXPERT, ELIXIR_LS, NONE }

enum class ExpertMode { AUTOMATIC, CUSTOM }

enum class ElixirLSMode { AUTOMATIC, CUSTOM }

@Service(Service.Level.PROJECT)
@State(name = "Elixir", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class ExSettings(
    internal val coroutineScope: CoroutineScope
) : SimplePersistentStateComponent<ExSettings.State>(State()), ModificationTracker {

    init {
        // In tests we must not auto-start LSP servers (or block on their responses) because
        // code highlighting runs in the fixture and can trigger LSP-backed passes such as
        // sticky lines / document symbols. Setting this to NONE in unit-test mode keeps
        // highlighting fast and deterministic, and prevents hangs when an LSP server is
        // unavailable or waiting on external resources.
        if (ApplicationManager.getApplication()?.isUnitTestMode == true) {
            state.codeIntelligenceService = CodeIntelligenceService.NONE
        }
    }

    class State : BaseState() {
        var elixirToolchainPath by string()
        var codeIntelligenceService by enum(CodeIntelligenceService.EXPERT)
        var expertMode by enum(ExpertMode.AUTOMATIC)
        var expertCustomExecutablePath by string()
        var elixirLSMode by enum(ElixirLSMode.AUTOMATIC)
        var elixirLSCustomExecutablePath by string()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ExSettings = project.service()
    }

    var elixirToolchainPath: String? by state::elixirToolchainPath
    var codeIntelligenceService: CodeIntelligenceService by state::codeIntelligenceService
    var expertMode: ExpertMode by state::expertMode
    var expertCustomExecutablePath: String? by state::expertCustomExecutablePath
    var elixirLSMode: ElixirLSMode by state::elixirLSMode
    var elixirLSCustomExecutablePath: String? by state::elixirLSCustomExecutablePath

    override fun getModificationCount(): Long = stateModificationCount
}
