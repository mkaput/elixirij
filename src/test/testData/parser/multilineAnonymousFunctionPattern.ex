# Minimal case: multiline anonymous function with pattern matching in call
expect(MockHTTP, :get, fn
  %{pre: [{BaseUrl, :call, ["https://api.example.com"]} | _]},
  "/endpoint",
  _opts ->
    {:ok, "result"}
end)
