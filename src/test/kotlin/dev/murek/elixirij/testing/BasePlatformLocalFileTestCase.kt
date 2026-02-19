package dev.murek.elixirij.testing

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div

/**
 * Base class for tests that need real local filesystem paths under IntelliJ's light test framework.
 *
 * Each test gets an isolated [localProjectRoot] under [projectBasePath], named by test fixture name.
 * The root is removed in [tearDown], with VFS refresh to avoid cross-test leakage.
 */
abstract class BasePlatformLocalFileTestCase : BasePlatformTestCase() {
    private lateinit var localProjectRootPath: Path

    /**
     * Physical base path of the light test project.
     *
     * This path can be reused across tests, so test data should be created under [localProjectRoot].
     */
    protected val projectBasePath: Path
        get() = Path.of(checkNotNull(project.basePath)).createDirectories()

    /**
     * Per-test isolated local filesystem root intended for creating files/directories in tests.
     */
    protected val localProjectRoot: Path
        get() = localProjectRootPath

    /**
     * Resolves a local path to [VirtualFile] and refreshes filesystem state first.
     */
    protected fun refreshAndFindVirtualFile(path: Path): VirtualFile = checkNotNull(
        LocalFileSystem.getInstance().refreshAndFindFileByPath(path.toString())
    ) { "Virtual file should be found for $path" }

    override fun setUp() {
        super.setUp()
        localProjectRootPath = (projectBasePath / fixtureName).createDirectories()
    }

    override fun tearDown() {
        try {
            if (::localProjectRootPath.isInitialized && localProjectRootPath.toFile().exists()) {
                localProjectRootPath.toFile().deleteRecursively()
                refreshAndFindVirtualFile(projectBasePath).refresh(false, true)
            }
        } finally {
            super.tearDown()
        }
    }
}
