# This is a comment
defmodule MyModule do
  @moduledoc "Module documentation"
  @version "1.0.0"

  def greet(name) do
    message = "Hello, #{name}!"
    IO.puts(message)
    :ok
  end

  defp private_function(x, y) do
    x + y * 2
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
    string = "hello world"
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
