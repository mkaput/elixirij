package dev.murek.elixirij.lsp

import com.intellij.util.messages.Topic

/**
 * Listener interface for Expert settings changes.
 *
 * This uses IntelliJ's message bus pattern for extensible event broadcasting.
 * Subscribe to [TOPIC] on the project message bus to receive notifications.
 *
 * Example subscription:
 * ```kotlin
 * project.messageBus.connect(disposable)
 *     .subscribe(ExpertSettingsListener.TOPIC, ExpertSettingsListener {
 *         // Handle the change
 *     })
 * ```
 */
fun interface ExpertSettingsListener {
    companion object {
        @JvmField
        @Topic.ProjectLevel
        val TOPIC = Topic(ExpertSettingsListener::class.java, Topic.BroadcastDirection.NONE)
    }

    /**
     * Called when Expert settings have changed.
     */
    fun settingsChanged()
}
