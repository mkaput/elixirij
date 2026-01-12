Enum.reduce([], fn {_, line} = current, [{_, prev_line} | _] = acc when line - 1 == prev_line ->
  [current | acc]
end)
