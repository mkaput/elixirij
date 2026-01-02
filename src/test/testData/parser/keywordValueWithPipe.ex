# Minimal case: multiline list with atom: after identifier (not a keyword pair)
defmodule MyApp.MixProject do
  def application do
    [
      extra_applications:
        [
          :logger,
          :runtime_tools,
          :crypto
        ]
        |> Enum.reject(&is_nil/1)
    ]
  end
