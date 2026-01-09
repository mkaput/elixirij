package dev.murek.elixirij.ide.highlighting

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ExHighlightingAnnotatorTest : BasePlatformTestCase() {

    fun `test defmodule keyword is highlighted`() {
        doTest("<info textAttributesKey=\"ELIXIR_SPECIAL_FORM\">defmodule</info> MyModule do\nend")
    }

    fun `test spec attribute above private function is highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              @<info textAttributesKey="ELIXIR_MODULE_ATTRIBUTE">spec</info> <info textAttributesKey="ELIXIR_FUNCTION_CALL">fetch</info>(Ecto.Queryable.<info textAttributesKey="ELIXIR_FUNCTION_CALL">t</info>(), <info textAttributesKey="ELIXIR_FUNCTION_CALL">any</info>(), <info textAttributesKey="ELIXIR_FUNCTION_CALL">keyword</info>()) ::
                      {:ok, Ecto.Schema.<info textAttributesKey="ELIXIR_FUNCTION_CALL">t</info>()} | {:error, <info textAttributesKey="ELIXIR_FUNCTION_CALL">atom</info>()}
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">defp</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">fetch</info>(
                    queryable,
                    id,
                    opts \\ []
                  ) do
                {on_error, opts} = Keyword.<info textAttributesKey="ELIXIR_FUNCTION_CALL">pop</info>(opts, :on_error, :not_found)

                <info textAttributesKey="ELIXIR_SPECIAL_FORM">case</info> get(<blank>queryable</blank>, id, opts) do
                  nil -> {:error, on_error}
                  record -> {:ok, record}
                end
              end
            end
        """.trimIndent()
        )
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
                <blank>x</blank> + <blank>y</blank>
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
                  <info textAttributesKey="ELIXIR_SPECIAL_FORM">unquote</info>(<blank>expr</blank>)
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
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">defguard</info> <info textAttributesKey="ELIXIR_MACRO_DECLARATION">is_positive</info>(<blank>x</blank>) when x > 0
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
                <info textAttributesKey="ELIXIR_SPECIAL_FORM">case</info> <blank>x</blank> do
                  :ok -> :success
                end
              end
            end
        """.trimIndent()
        )
    }

    fun `test function calls are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info> do
                <info textAttributesKey="ELIXIR_FUNCTION_CALL">foo</info>(1)
                Foo.Bar.<info textAttributesKey="ELIXIR_FUNCTION_CALL">baz</info>(2)
                <blank>foo</blank>.(3)
              end
            end
        """.trimIndent()
        )
    }

    fun `test bracket access does not highlight receiver as a function call`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info>(opts, query) do
                Ecto.Adapters.SQL.<info textAttributesKey="ELIXIR_FUNCTION_CALL">explain</info>(<blank>opts</blank>[:repo], opts[:query_fn], query, opts)
              end
            end
        """.trimIndent()
        )
    }

    fun `test qualified call with predicate suffix is highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info> do
                <info textAttributesKey="ELIXIR_SPECIAL_FORM">if</info> Code.<info textAttributesKey="ELIXIR_FUNCTION_CALL">ensure_loaded?</info>(Ecto.DevLogger) do
                  :ok
                end
              end
            end
        """.trimIndent()
        )
    }

    fun `test predicate calls are highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info>(results, partition_str) do
                <info textAttributesKey="ELIXIR_FUNCTION_CALL">assert</info> <info textAttributesKey="ELIXIR_FUNCTION_CALL">is_function</info>(<blank>results</blank>)

                <info textAttributesKey="ELIXIR_SPECIAL_FORM">if</info> <info textAttributesKey="ELIXIR_FUNCTION_CALL">is_binary</info>(<blank>partition_str</blank>) do
                  :ok
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
                  <blank>a</blank>
                end
              end
            end
        """.trimIndent()
        )
    }

    fun `test bracket access does not highlight atom key`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info>(base_params, tags) do
                Map.<info textAttributesKey="ELIXIR_FUNCTION_CALL">merge</info>(base_params, tags[<blank>:admin</blank>])
              end
            end
        """.trimIndent()
        )
    }

    fun `test bracket access atom key inside calls is not highlighted`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info>(base_params, tags) do
                <info textAttributesKey="ELIXIR_SPECIAL_FORM">if</info> <info textAttributesKey="ELIXIR_FUNCTION_CALL">is_map</info>(tags[<blank>:admin</blank>]) do
                  Map.<info textAttributesKey="ELIXIR_FUNCTION_CALL">merge</info>(base_params, tags[<blank>:admin</blank>])
                end
              end
            end
        """.trimIndent()
        )
    }

    fun `test binary expr does not highlight left operand`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">example</info>(<blank>count</blank>, <blank>chunk</blank>) do
                :ets.insert(processed, {:count, <blank>count</blank> + <info textAttributesKey="ELIXIR_FUNCTION_CALL">length</info>(chunk)})
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
                  <blank>i</blank> * 2
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
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">my_func</info>(<blank>t</blank>)
            end

            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defimpl</info> MyProtocol, for: MyStruct do
              <info textAttributesKey="ELIXIR_SPECIAL_FORM">def</info> <info textAttributesKey="ELIXIR_FUNCTION_DECLARATION">my_func</info>(t), do: <blank>t</blank>.name
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
                  {:msg, <blank>x</blank>} -> x
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
                <info textAttributesKey="ELIXIR_SPECIAL_FORM">__MODULE__</info> = _arg
                __using__ = _arg
                _ARG = _arg
                _arg
              end
            end
        """.trimIndent()
        )
    }

    fun `test incomplete call highlighting does not throw`() {
        myFixture.configureByText(
            "test.ex",
            """
            defmodule MyModule do
              foo.(
            end
            """.trimIndent()
        )
        myFixture.doHighlighting()
    }

    fun `test parser errors do not highlight atom call targets`() {
        doTest(
            """
            <info textAttributesKey="ELIXIR_SPECIAL_FORM">defmodule</info> MyModule do
              <blank>:bar</blank>(
            end
            """.trimIndent()
        )
    }

    private fun doTest(code: String) {
        val blankPattern = Regex("<blank>(.*?)</blank>", setOf(RegexOption.DOT_MATCHES_ALL))
        val blankRanges = mutableListOf<IntRange>()
        var stripped = code
        while (true) {
            val match = blankPattern.find(stripped) ?: break
            val content = match.groupValues[1]
            val start = match.range.first
            blankRanges.add(start until (start + content.length))
            stripped = stripped.replaceRange(match.range, content)
        }
        myFixture.configureByText("test.ex", stripped)
        myFixture.checkHighlighting(false, true, false, true)
        if (blankRanges.isNotEmpty()) {
            val highlights = myFixture.doHighlighting()
            for (range in blankRanges) {
                val hasAnyHighlight = highlights.any { info ->
                    val start = info.startOffset
                    val end = info.endOffset
                    start <= range.last && end >= range.first + 1
                }
                assertFalse(
                    "Expected no highlighting for blank range at ${range.first}-${range.last}",
                    hasAnyHighlight
                )
            }
        }
    }
}
