fn
  %Client{pre: [{BaseUrl, :call, ["https://example.com"]} | _]},
  "/v1",
  _opts ->
    :ok
end
