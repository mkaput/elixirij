package dev.murek.elixirij

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.VisibleForTesting

enum class ExpertMode { DISABLED, AUTOMATIC, CUSTOM }
enum class ExpertReleaseChannel { STABLE, NIGHTLY }

@Service(Service.Level.PROJECT)
@State(name = "Elixir", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class ExSettings(
    internal val coroutineScope: CoroutineScope
) : SimplePersistentStateComponent<ExSettings.State>(State()), ModificationTracker {

    init {
        // In tests, we must not auto-start LSP servers (or block on their responses) because
        // code highlighting runs in the fixture and can trigger LSP-backed passes such as
        // sticky lines / document symbols. Disabling Expert in unit-test mode keeps
        // highlighting fast and deterministic and prevents hangs when the server is
        // unavailable or waiting on external resources.
        applyTestDefaultsIfNeeded()
    }

    class State : BaseState() {
        var elixirToolchainPath by string()
        var expertMode by enum(ExpertMode.AUTOMATIC)
        var expertReleaseChannel by enum(ExpertReleaseChannel.STABLE)
        var expertCustomExecutablePath by string()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ExSettings = project.service()
    }

    var elixirToolchainPath: String? by state::elixirToolchainPath
    var expertMode: ExpertMode by state::expertMode
    var expertReleaseChannel: ExpertReleaseChannel by state::expertReleaseChannel
    var expertCustomExecutablePath: String? by state::expertCustomExecutablePath

    override fun loadState(state: State) {
        super.loadState(state)
        applyTestDefaultsIfNeeded()
    }

    override fun getModificationCount(): Long = stateModificationCount

    @VisibleForTesting
    fun reset() {
        state.elixirToolchainPath = null
        state.expertCustomExecutablePath = null
        state.expertMode = ExpertMode.DISABLED
        state.expertReleaseChannel = ExpertReleaseChannel.STABLE
    }

    private fun applyTestDefaultsIfNeeded() {
        val app = ApplicationManager.getApplication()
        val isUnitTestProperty = System.getProperty("idea.is.unit.test") == "true"
        if (isUnitTestProperty || app?.isUnitTestMode == true || app?.isHeadlessEnvironment == true) {
            state.expertMode = ExpertMode.DISABLED
        }
    }
}
