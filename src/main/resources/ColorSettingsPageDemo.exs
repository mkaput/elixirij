# This is a comment
<SPECIAL_FORM>defmodule</SPECIAL_FORM> MyModule do
  <DOC_COMMENT>@moduledoc "Module documentation"</DOC_COMMENT>
  @<MODULE_ATTRIBUTE>version</MODULE_ATTRIBUTE> "1.0.0"
  @<MODULE_ATTRIBUTE>behaviour</MODULE_ATTRIBUTE> GenServer

  <SPECIAL_FORM>require</SPECIAL_FORM> Logger
  <SPECIAL_FORM>import</SPECIAL_FORM> Enum, only: [map: 2]
  <SPECIAL_FORM>alias</SPECIAL_FORM> MyModule.Helper

  <SPECIAL_FORM>defstruct</SPECIAL_FORM> [:name, :count]

  <SPECIAL_FORM>def</SPECIAL_FORM> <FUNCTION_DECLARATION>greet</FUNCTION_DECLARATION>(name) do
    message = "Hello, #{name}!"
    IO.puts(message)
    :ok
  end

  <SPECIAL_FORM>defp</SPECIAL_FORM> <FUNCTION_DECLARATION>private_function</FUNCTION_DECLARATION>(x, y) do
    x + y * 2
  end

  <SPECIAL_FORM>defmacro</SPECIAL_FORM> <MACRO_DECLARATION>my_macro</MACRO_DECLARATION>(expr) do
    <SPECIAL_FORM>quote</SPECIAL_FORM> do
      <SPECIAL_FORM>unquote</SPECIAL_FORM>(expr) + 1
    end
  end

  <SPECIAL_FORM>def</SPECIAL_FORM> <FUNCTION_DECLARATION>control_flow</FUNCTION_DECLARATION>(x) do
    <SPECIAL_FORM>case</SPECIAL_FORM> x do
      :ok -> :success
      :error -> :failure
    end

    <SPECIAL_FORM>with</SPECIAL_FORM> {:ok, a} <- fetch_a(),
         {:ok, b} <- fetch_b(a) do
      {:ok, a + b}
    end

    <SPECIAL_FORM>for</SPECIAL_FORM> i <- 1..10, rem(i, 2) == 0, do: i * 2
  end

  <SPECIAL_FORM>def</SPECIAL_FORM> <FUNCTION_DECLARATION>using_sigil</FUNCTION_DECLARATION> do
    ~r/pattern/i
    ~s(string sigil)
  end

  <SPECIAL_FORM>def</SPECIAL_FORM> <FUNCTION_DECLARATION>literals</FUNCTION_DECLARATION> do
    integer = 42
    float = 3.14
    hex = 0xFF
    bin = 0b1010
    char = ?a
    atom = :my_atom
    quoted_atom = :"quoted atom"
    string = "hello\\nworld \\u00A9 \\xFF"
    charlist = 'charlist'
    list = [1, 2, 3]
    tuple = {1, 2, 3}
    map = %{key: "value"}
    bitstring = <<1, 2, 3>>
  end

  <SPECIAL_FORM>def</SPECIAL_FORM> <FUNCTION_DECLARATION>operators</FUNCTION_DECLARATION> do
    result = 1 + 2 - 3 * 4 / 5
    comparison = a == b and c != d
    pipe = data |> transform() |> output()
    range = 1..10
  end

  <SPECIAL_FORM>def</SPECIAL_FORM> <FUNCTION_DECLARATION>unused_variable</FUNCTION_DECLARATION> do
    <UNUSED_VARIABLE>_unused</UNUSED_VARIABLE> = 1
    :ok
  end
end
