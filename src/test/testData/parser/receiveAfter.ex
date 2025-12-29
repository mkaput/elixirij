receive do
  {:msg, x} -> x
after
  1000 -> :timeout
end
