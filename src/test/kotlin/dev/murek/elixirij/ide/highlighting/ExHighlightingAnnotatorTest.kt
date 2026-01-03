package dev.murek.elixirij.ide.highlighting

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ExHighlightingAnnotatorTest : BasePlatformTestCase() {

    fun `test defmodule keyword is highlighted`() {
        doTest("<info textAttributesKey=\"ELIXIR_SPECIAL_FORM\">defmodule</info> MyModule do\nend")
    }

    fun `test def keyword and function name are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">my_function</info> do
                :ok
              end
            end
        """.trimIndent()
        )
    }

    fun `test defp keyword and function name are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">defp</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">private_function</info>(x, y) do
                x + y
              end
            end
        """.trimIndent()
        )
    }

    fun `test defmacro keyword and macro name are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmacro</info> <info textAttributesKey="ELIXIR_MACRO_DECLARATION">my_macro</info>(expr) do
                <info textAttributesKey="ELIXIR_SPECIAL_FORM">quote</info> do
                  <info textAttributesKey="ELIXIR_SPECIAL_FORM">unquote</info>(expr)
                end
              end
            end
        """.trimIndent()
        )
    }

    fun `test defguard keyword and guard name are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">defguard</info> <info textAttributesKey="ELIXIR_MACRO_DECLARATION">is_positive</info>(x) when x > 0
            end
        """.trimIndent()
        )
    }

    fun `test module attributes are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_DOC_COMMENT">@moduledoc "Documentation"</info>
              @<info textAttributesKey="ELIXIR_MODULE_ATTRIBUTE">version</info> "1.0.0"
              @<info textAttributesKey="ELIXIR_MODULE_ATTRIBUTE">behaviour</info> GenServer
            end
        """.trimIndent()
        )
    }

    fun `test doc attributes are highlighted as documentation comments`() {
        val heredoc = "\"\"\""
        doTest(
            """
                <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
                  <info textAttributesKey="ELIXIR_DOC_COMMENT">@moduledoc $heredoc
                  Module docs.
                  $heredoc</info>

                  <info textAttributesKey="ELIXIR_DOC_COMMENT">@doc "Function docs."</info>
                  <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info>, do: :ok
                end
            """.trimIndent()
        )
    }

    fun `test control flow keywords are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info>(x) do
                <info textAttributesKey="ELIXIR_SPECIAL_FORM">case</info> x do
                  :ok -> :success
                end
              end
            end
        """.trimIndent()
        )
    }

    fun `test with expression is highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info> do
                <info textAttributesKey="ELIXIR_SPECIAL_FORM">with</info> {:ok, a} <- fetch() do
                  a
                end
              end
            end
        """.trimIndent()
        )
    }

    fun `test for comprehension is highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info> do
                <info textAttributesKey="ELIXIR_SPECIAL_FORM">for</info> i <- 1..10 do
                  i * 2
                end
              end
            end
        """.trimIndent()
        )
    }

    fun `test import require use alias are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">require</info> Logger
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">import</info> Enum
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">use</info> GenServer
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">alias</info> MyModule.Helper
            end
        """.trimIndent()
        )
    }

    fun `test quote and unquote are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmacro</info> <info textAttributesKey="ELIXIR_MACRO_DECLARATION">my_macro</info>(expr) do
                <info textAttributesKey="ELIXIR_SPECIAL_FORM">quote</info> do
                  <info textAttributesKey="ELIXIR_SPECIAL_FORM">unquote</info>(expr) + 1
                end
              end
            end
        """.trimIndent()
        )
    }

    fun `test defstruct and defexception are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyStruct do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">defstruct</info> [:name, :count]
            end

            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyError do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">defexception</info> [:message]
            end
        """.trimIndent()
        )
    }

    fun `test defprotocol and defimpl are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defprotocol</info> MyProtocol do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">my_func</info>(t)
            end

            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defimpl</info> MyProtocol, for: MyStruct do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">my_func</info>(t), do: t.name
            end
        """.trimIndent()
        )
    }

    fun `test try receive cond are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info> do
                <info textAttributesKey="ELIXIR_SPECIAL_FORM">try</info> do
                  :ok
                rescue
                  _ -> :error
                end

                <info textAttributesKey="ELIXIR_SPECIAL_FORM">receive</info> do
                  {:msg, x} -> x
                end

                <info textAttributesKey="ELIXIR_SPECIAL_FORM">cond</info> do
                  true -> :ok
                end
              end
            end
        """.trimIndent()
        )
    }

    fun `test unused identifiers are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">_private</info>(<info textAttributesKey="ELIXIR_UNUSED_VARIABLE">_arg</info>) do
                <info textAttributesKey="ELIXIR_UNUSED_VARIABLE">_</info> = _arg
                <info textAttributesKey="ELIXIR_UNUSED_VARIABLE">_unused</info> = _arg
                __MODULE__ = _arg
                _ARG = _arg
                _arg
              end
            end
        """.trimIndent()
        )
    }

    private fun doTest(code: String) {
        myFixture.configureByText("test.ex", code)
        myFixture.checkHighlighting(false, true, false, true)
    }
}
