package dev.murek.elixirij.lsp

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

/**
 * The entry-point interface for getting the Expert LS instance for a project.
 */
@Service(Service.Level.PROJECT)
class Expert(private val project: Project, private val cs: CoroutineScope) {
    companion object {
        @JvmStatic
        fun getInstance(project: Project): Expert = project.service()
    }

    private val settings = ExLspSettings.getInstance(project)

    /**
     * Get a ready to use Expert executable for this project, or `null` if none is available.
     *
     * If returned `null` and you're in an interactive context (e.g., user clicked a button), consider calling
     * [checkUpdates] to let the user download and install Expert. All of this is wrapped in
     * [withCurrentExecutableOrRequestSetup] nicely.
     */
    fun currentExecutable(): Path? {
        return when (settings.expertMode) {
            ExpertMode.DISABLED -> null
            ExpertMode.AUTOMATIC -> null // TODO("We don't manage local Expert installs yet.")
            ExpertMode.CUSTOM -> {
                settings.expertCustomExecutablePath?.let { Path(it) }?.takeIf { it.exists() }
            }
        }
    }

    /**
     * Trigger update check/fresh install if needed and permitted by the user for this project.
     */
    fun checkUpdates() {
        if (settings.expertMode == ExpertMode.AUTOMATIC) {
            // TODO("We don't manage local Expert installs yet.")
        }
    }

    /**
     * Convenience method for working with [currentExecutable], to use in interactive contexts only.
     *
     * Note that if the user has disabled Expert support for this project in settings, this method will neither
     * call the `block` nor request setup.
     */
    inline fun <T> withCurrentExecutableOrRequestSetup(block: (Path) -> T): T? =
        when (val executable = currentExecutable()) {
            null -> checkUpdates().let { null }
            else -> block(executable)
        }
}
