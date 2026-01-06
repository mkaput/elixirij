# This is a comment
defmodule MyModule do
  @moduledoc "Module documentation"
  @version "1.0.0"
  @behaviour GenServer

  require Logger
  import Enum, only: [map: 2]
  alias MyModule.Helper

  defstruct [:name, :count]

  def greet(name) do
    message = "Hello, #{name}!"
    IO.puts(message)
    :ok
  end

  defp private_function(x, y) do
    x + y * 2
  end

  defmacro my_macro(expr) do
    quote do
      unquote(expr) + 1
    end
  end

  def control_flow(x) do
    case x do
      :ok -> :success
      :error -> :failure
    end

    with {:ok, a} <- fetch_a(),
         {:ok, b} <- fetch_b(a) do
      {:ok, a + b}
    end

    for i <- 1..10, rem(i, 2) == 0, do: i * 2
  end

  def using_sigil do
    ~r/pattern/i
    ~s(string sigil)
  end

  def literals do
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

  def operators do
    result = 1 + 2 - 3 * 4 / 5
    comparison = a == b and c != d
    pipe = data |> transform() |> output()
    range = 1..10
  end
end
