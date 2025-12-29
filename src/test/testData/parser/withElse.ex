with {:ok, x} <- f() do
  x
else
  _ -> :err
end
