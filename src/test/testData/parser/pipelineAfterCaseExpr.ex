value
|> case do
  {:ok, v} -> v
  _ -> nil
end
|> Keyword.get(:result)
