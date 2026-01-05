fn
  {:ok, value} = ast, acc
  when is_atom(value) ->
    {ast, acc}
end
