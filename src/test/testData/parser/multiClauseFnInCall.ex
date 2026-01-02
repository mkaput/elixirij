Enum.all?(results, fn
  {:ok, _} -> true
  _ -> false
end)
