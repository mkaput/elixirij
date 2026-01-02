session
|> select("#format", ["value"],
  exact_option: true,
  from: nil
)
|> click("#submit")
