package dev.murek.elixirij.ide

import com.intellij.lang.BracePair
import com.intellij.lang.PairedBraceMatcher
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import dev.murek.elixirij.lang.EX_DO
import dev.murek.elixirij.lang.EX_END
import dev.murek.elixirij.lang.EX_FN
import dev.murek.elixirij.lang.EX_GT_GT
import dev.murek.elixirij.lang.EX_INTERPOLATION_END
import dev.murek.elixirij.lang.EX_INTERPOLATION_START
import dev.murek.elixirij.lang.EX_LBRACE
import dev.murek.elixirij.lang.EX_LBRACKET
import dev.murek.elixirij.lang.EX_LPAREN
import dev.murek.elixirij.lang.EX_LT_LT
import dev.murek.elixirij.lang.EX_PERCENT_LBRACE
import dev.murek.elixirij.lang.EX_RBRACE
import dev.murek.elixirij.lang.EX_RBRACKET
import dev.murek.elixirij.lang.EX_RPAREN
import dev.murek.elixirij.lang.psi.ExTypes

/**
 * Provides brace matching support for Elixir delimiters.
 *
 * Highlights matching pairs when the cursor is positioned near a delimiter:
 * - do...end blocks
 * - fn...end expressions
 * - Parentheses: ()
 * - Brackets: []
 * - Braces: {}
 * - Map literals: %{}
 * - Binary delimiters: <<>>
 * - Interpolation braces: #{}
 */
class ExBraceMatcher : PairedBraceMatcher {
    private val pairs = arrayOf(
        BracePair(EX_DO, EX_END, true),
        BracePair(EX_FN, EX_END, true),
        BracePair(EX_LPAREN, EX_RPAREN, false),
        BracePair(EX_LBRACKET, EX_RBRACKET, false),
        BracePair(EX_LBRACE, EX_RBRACE, false),
        BracePair(EX_PERCENT_LBRACE, EX_RBRACE, false),
        BracePair(EX_LT_LT, EX_GT_GT, false),
        BracePair(EX_INTERPOLATION_START, EX_INTERPOLATION_END, false)
    )

    override fun getPairs(): Array<BracePair> = pairs

    override fun isPairedBracesAllowedBeforeType(
        lbraceType: IElementType,
        contextType: IElementType?
    ): Boolean = true

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int {
        val element = file.findElementAt(openingBraceOffset) ?: return openingBraceOffset
        val parent = element.parent ?: return openingBraceOffset
        val parentNode = parent.node ?: return openingBraceOffset

        // For do blocks, navigate to the outermost call expression
        if (parentNode.elementType == ExTypes.DO_BLOCK) {
            var current = parent.parent
            var outermostCall = current

            // Traverse up through nested call expressions to find the outermost one
            while (current != null) {
                val elementType = current.node?.elementType
                if (elementType == ExTypes.CALL_EXPR ||
                    elementType == ExTypes.ACCESS_EXPR
                ) {
                    outermostCall = current
                }
                current = current.parent
            }

            if (outermostCall != null) {
                return outermostCall.textOffset
            }
        }

        return openingBraceOffset
    }
}
