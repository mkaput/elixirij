# Minimal case: anonymous function capture with function call expression
Enum.all?(results, &match?({:ok, "PONG"}, &1))
