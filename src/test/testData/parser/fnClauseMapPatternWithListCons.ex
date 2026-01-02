fn
  %Client{pre: [{BaseUrl, :call, ["https://api.example.com"]} | _]},
  "/v1/coupons?limit=100",
  _opts ->
    :ok
end
