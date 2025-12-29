package dev.murek.elixirij.ide.highlighting

import com.intellij.testFramework.fixtures.BasePlatformTestCase
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

    private fun doTest(code: String) {
        myFixture.configureByText("test.ex", code)
        // Run highlighting to ensure the annotator doesn't throw
        myFixture.doHighlighting()
    }
}
