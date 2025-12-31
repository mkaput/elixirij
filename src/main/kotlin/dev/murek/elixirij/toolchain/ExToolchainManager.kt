package dev.murek.elixirij.toolchain

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import dev.murek.elixirij.ExSettings
import kotlinx.coroutines.CoroutineScope
import kotlin.io.path.Path

/**
 * Returns the configured toolchain for this project.
 *
 * This is the preferred way to access the toolchain.
 */
val Project.elixirToolchain: ExToolchain? get() = ExToolchainManager.getInstance(this).toolchain


@Service(Service.Level.PROJECT)
class ExToolchainManager(project: Project, private val cs: CoroutineScope) {

    companion object {
        @JvmStatic
        fun getInstance(project: Project): ExToolchainManager = project.service()
    }

    private val settings = ExSettings.getInstance(project)
    private val cachedValuesManager = CachedValuesManager.getManager(project)

    private val toolchainCache = cachedValuesManager.createCachedValue {
        val toolchain =
            settings.elixirToolchainPath?.let { ExToolchain.createValid(Path(it)).getOrNull() } ?: detectedToolchain

        CachedValueProvider.Result.create(toolchain, settings)
    }

    /**
     * Returns the configured toolchain.
     * If [ExSettings.elixirToolchainPath] is null, auto-detects from common locations.
     * If a custom path is set, uses that path.
     * The resolved toolchain is cached but is recomputed if settings change, therefore, do not persist the value.
     * The resolved toolchain is guaranteed to be valid.
     */
    val toolchain: ExToolchain? get() = toolchainCache.value

    /**
     * Returns the detected toolchain for this project.
     *
     * User might override the detected toolchain; therefore, you will almost always want to use [toolchain] property
     * instead.
     */
    val detectedToolchain: ExToolchain? by lazy { detectElixirToolchain() }
}
