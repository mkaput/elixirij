package dev.murek.elixirij.toolchain

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.platform.util.coroutines.childScope
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.murek.elixirij.ExBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.FlowLayout
import kotlin.io.path.Path
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingConstants

internal class ExExecutableTestButton(
    private val project: Project,
    private val fieldToUpdate: TextFieldWithBrowseButton,
    private val toolchainProvider: ExToolchainManager,
    parentScope: CoroutineScope,
    parentDisposable: Disposable?,
) : JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)) {

    private val scope = parentScope.childScope("ExExecutableTestButton")
    private val button = JButton()
    private val resultLabel = JBLabel("", AllIcons.Empty, SwingConstants.LEFT)
    private val spinnerIcon: AsyncProcessIcon = createSpinnerIcon(parentDisposable)

    var text: @NlsContexts.Label String
        get() = button.text
        set(value) {
            button.text = value
            button.revalidate()
        }

    private var autoDetectedVersion: String? = null

    init {
        resultLabel.border = JBUI.Borders.emptyLeft(UIUtil.DEFAULT_HGAP)

        add(button)
        add(spinnerIcon)
        add(resultLabel)

        parentDisposable?.let { disposable ->
            Disposer.register(disposable) { scope.cancel() }
        }

        button.addActionListener {
            spinnerIcon.isVisible = true
            spinnerIcon.requestFocus()
            button.isEnabled = false
            resultLabel.text = ExBundle.message("configurable.toolchain.testResultLabel.progressTitle")
            resultLabel.icon = AllIcons.Actions.BuildLoadChanges

            val (result, exception) = try {
                runTestBlocking(resultLabel.text) to null
            } catch (exception: Exception) {
                null to exception
            }

            resultLabel.text = if (result.isNullOrEmpty()) {
                ExBundle.message("configurable.toolchain.testResultLabel.not.found")
            } else {
                result
            }
            resultLabel.icon = when {
                !result.isNullOrEmpty() -> AllIcons.General.InspectionsOK
                exception != null || result.isNullOrEmpty() -> AllIcons.General.BalloonWarning
                else -> AllIcons.Empty
            }

            spinnerIcon.isVisible = false
            button.isEnabled = true
            button.requestFocus()
        }

        refreshDetectedVersionIfNeeded()
        updateTestButton(fieldToUpdate.text)
    }

    private fun createSpinnerIcon(parentDisposable: Disposable?): AsyncProcessIcon {
        return AsyncProcessIcon("ExToolchainDetectionProgress").apply {
            border = JBUI.Borders.emptyLeft(UIUtil.DEFAULT_HGAP)
            isVisible = false
            parentDisposable?.let { Disposer.register(it, this) }
        }
    }

    private fun refreshDetectedVersionIfNeeded() {
        if (!fieldToUpdate.text.isNullOrBlank()) {
            return
        }

        val detectedToolchain = toolchainProvider.detectedToolchain
        if (detectedToolchain == null) {
            updateTestButton(fieldToUpdate.text)
            return
        }

        scope.launch {
            val version = runCatching { detectedToolchain.fetchVersion(project) }.getOrNull()
            withContext(Dispatchers.EDT) {
                autoDetectedVersion = version
                updateTestButton(fieldToUpdate.text)
            }
        }
    }

    private fun runTestBlocking(title: @NlsContexts.ProgressTitle String): String = runWithModalProgressBlocking(
        owner = ModalTaskOwner.component(this),
        title = title,
    ) {
        val (toolchain, toolPath) = resolveToolchain()
        withContext(Dispatchers.EDT) { updateTestButton(toolPath) }
        toolchain?.fetchVersion(project).orEmpty()
    }

    private suspend fun resolveToolchain(): Pair<ExToolchain?, String?> {
        val currentPath = withContext(Dispatchers.EDT) { fieldToUpdate.text }

        if (!currentPath.isNullOrBlank()) {
            val toolchain = ExToolchain.createValid(Path(currentPath)).getOrNull()
            return toolchain to currentPath
        }

        val detected = toolchainProvider.detectedToolchain
        return detected to detected?.elixir?.toString()
    }

    fun updateTestButton(toolPath: String?): String {
        val detectedVersion = autoDetectedVersion
        val hasDetectedToolchain = toolchainProvider.detectedToolchain != null
        text = ExBundle.message("configurable.toolchain.testButton.text")
        if (toolPath.isNullOrEmpty()) {
            if (detectedVersion != null) {
                resultLabel.icon = AllIcons.General.InspectionsOK
                resultLabel.text = detectedVersion
            } else if (!hasDetectedToolchain) {
                resultLabel.icon = AllIcons.General.BalloonWarning
                resultLabel.text = ExBundle.message("configurable.toolchain.testResultLabel.not.found")
            } else {
                resultLabel.icon = AllIcons.Empty
                resultLabel.text = ""
            }
        } else {
            autoDetectedVersion = null
            resultLabel.icon = AllIcons.Empty
            resultLabel.text = ""
        }
        return toolPath ?: ""
    }
}
