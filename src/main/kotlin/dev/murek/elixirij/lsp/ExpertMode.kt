package dev.murek.elixirij.lsp

/**
 * Determines how the Expert language server is configured for a project.
 */
enum class ExpertMode {
    /**
     * The user doesn't want Expert support in this project.
     */
    DISABLED,

    /**
     * The user wants the plugin to automatically manage Expert installation and updates.
     */
    AUTOMATIC,

    /**
     * The user wants to provide their own Expert executable via [ExpertSettings.customExecutablePath].
     */
    CUSTOM
}
