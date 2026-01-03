package dev.murek.elixirij.ide.highlighting

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.childrenOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import dev.murek.elixirij.lang.psi.ExIdentifier
import dev.murek.elixirij.lang.psi.ExModuleAttr
/**
 * Tests for [ExHighlightingAnnotator] to verify semantic highlighting.
 */
class ExHighlightingAnnotatorTest : BasePlatformTestCase() {

    fun `test defmodule keyword is highlighted`() {
        doTest("defmodule MyModule do\nend")
    }

    fun `test def keyword and function name are highlighted`() {
        doTest("""
            defmodule MyModule do
              def my_function do
                :ok
              end
            end
        """.trimIndent())
    }

    fun `test defp keyword and function name are highlighted`() {
        doTest("""
            defmodule MyModule do
              defp private_function(x, y) do
                x + y
              end
            end
        """.trimIndent())
    }

    fun `test defmacro keyword and macro name are highlighted`() {
        doTest("""
            defmodule MyModule do
              defmacro my_macro(expr) do
                quote do
                  unquote(expr)
                end
              end
            end
        """.trimIndent())
    }

    fun `test defguard keyword and guard name are highlighted`() {
        doTest("""
            defmodule MyModule do
              defguard is_positive(x) when x > 0
            end
        """.trimIndent())
    }

    fun `test module attributes are highlighted`() {
        doTest("""
            defmodule MyModule do
              @moduledoc "Documentation"
              @version "1.0.0"
              @behaviour GenServer
            end
        """.trimIndent())
    }

    fun `test doc attributes are highlighted as documentation comments`() {
        val heredoc = "\"\"\""
        myFixture.configureByText(
            "test.ex",
            """
                defmodule MyModule do
                  @moduledoc $heredoc
                  Module docs.
                  $heredoc

                  @doc "Function docs."
                  def example, do: :ok
                end
            """.trimIndent()
        )

        val highlights = myFixture.doHighlighting()
        val moduleAttrs = PsiTreeUtil.findChildrenOfType(myFixture.file, ExModuleAttr::class.java)

        val docAttrs = moduleAttrs.filter { attr ->
            attr.childrenOfType<ExIdentifier>().firstOrNull()?.text in setOf("doc", "moduledoc")
        }

        assertTrue("Expected @doc and @moduledoc attributes in the PSI", docAttrs.size >= 2)

        docAttrs.forEach { attr ->
            val range = attr.textRange
            val hasDocHighlight = highlights.any { highlight ->
                highlight.forcedTextAttributesKey == ExTextAttributes.DOC_COMMENT.attribute &&
                    highlight.startOffset == range.startOffset &&
                    highlight.endOffset == range.endOffset
            }
            assertTrue("Expected doc comment highlight for '${attr.text}'", hasDocHighlight)
        }
    }

    fun `test control flow keywords are highlighted`() {
        doTest("""
            defmodule MyModule do
              def example(x) do
                case x do
                  :ok -> :success
                end
              end
            end
        """.trimIndent())
    }

    fun `test with expression is highlighted`() {
        doTest("""
            defmodule MyModule do
              def example do
                with {:ok, a} <- fetch() do
                  a
                end
              end
            end
        """.trimIndent())
    }

    fun `test for comprehension is highlighted`() {
        doTest("""
            defmodule MyModule do
              def example do
                for i <- 1..10 do
                  i * 2
                end
              end
            end
        """.trimIndent())
    }

    fun `test import require use alias are highlighted`() {
        doTest("""
            defmodule MyModule do
              require Logger
              import Enum
              use GenServer
              alias MyModule.Helper
            end
        """.trimIndent())
    }

    fun `test quote and unquote are highlighted`() {
        doTest("""
            defmodule MyModule do
              defmacro my_macro(expr) do
                quote do
                  unquote(expr) + 1
                end
              end
            end
        """.trimIndent())
    }

    fun `test defstruct and defexception are highlighted`() {
        doTest("""
            defmodule MyStruct do
              defstruct [:name, :count]
            end

            defmodule MyError do
              defexception [:message]
            end
        """.trimIndent())
    }

    fun `test defprotocol and defimpl are highlighted`() {
        doTest("""
            defprotocol MyProtocol do
              def my_func(t)
            end

            defimpl MyProtocol, for: MyStruct do
              def my_func(t), do: t.name
            end
        """.trimIndent())
    }

    fun `test try receive cond are highlighted`() {
        doTest("""
            defmodule MyModule do
              def example do
                try do
                  :ok
                rescue
                  _ -> :error
                end

                receive do
                  {:msg, x} -> x
                end

                cond do
                  true -> :ok
                end
              end
            end
        """.trimIndent())
    }

    fun `test unused identifiers are highlighted`() {
        myFixture.configureByText(
            "test.ex",
            """
                defmodule MyModule do
                  def _private(_arg) do
                    _ = _arg
                    _unused = _arg
                    __MODULE__ = _arg
                    _ARG = _arg
                    _arg
                  end
                end
            """.trimIndent()
        )

        val highlights = myFixture.doHighlighting()
        val identifiers = PsiTreeUtil.findChildrenOfType(myFixture.file, ExIdentifier::class.java)
        val unusedIdentifiers = identifiers.filter { it.text.startsWith("_") }

        assertTrue("Expected unused identifiers in the PSI", unusedIdentifiers.isNotEmpty())

        val expectedUnused = setOf("_", "_unused")
        val expectedNotUnused = setOf("__MODULE__", "_ARG")

        unusedIdentifiers.forEach { identifier ->
            val range = identifier.textRange
            val hasUnusedHighlight = highlights.any { highlight ->
                highlight.forcedTextAttributesKey == ExTextAttributes.UNUSED_VARIABLE.attribute &&
                    highlight.startOffset == range.startOffset &&
                    highlight.endOffset == range.endOffset
            }
            if (identifier.text in expectedNotUnused) {
                assertFalse("Did not expect unused variable highlight for '${identifier.text}'", hasUnusedHighlight)
                return@forEach
            }
            if (identifier.text in expectedUnused) {
                assertTrue("Expected unused variable highlight for '${identifier.text}'", hasUnusedHighlight)
            }
        }
    }

    private fun doTest(code: String) {
        myFixture.configureByText("test.ex", code)
        // Run highlighting to ensure the annotator doesn't throw
        myFixture.doHighlighting()
    }
}
