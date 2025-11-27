package dev.murek.elixirij.commenter

import com.intellij.lang.Commenter

/**
 * Commenter for Elixir language.
 *
 * Elixir uses `#` for single-line comments and does not have block comments.
 */
class ExCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "# "

    override fun getBlockCommentPrefix(): String? = null

    override fun getBlockCommentSuffix(): String? = null

    override fun getCommentedBlockCommentPrefix(): String? = null

    override fun getCommentedBlockCommentSuffix(): String? = null
}
