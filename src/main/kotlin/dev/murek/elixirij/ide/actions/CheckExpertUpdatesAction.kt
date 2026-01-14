package dev.murek.elixirij.ide.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import dev.murek.elixirij.ExSettings
import dev.murek.elixirij.ExpertMode
import dev.murek.elixirij.lsp.Expert

class CheckExpertUpdatesAction : DumbAwareAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isVisible = project != null
        e.presentation.isEnabled =
            project != null && ExSettings.getInstance(project).expertMode != ExpertMode.DISABLED
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (ExSettings.getInstance(project).expertMode == ExpertMode.DISABLED) return
        Expert.getInstance(project).checkUpdates(force = true)
    }
}
