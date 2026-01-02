# Minimal case: anonymous capture with nested attribute access
list_cookies(conn, urls)
|> Enum.find(&(&1.name == "cookie-name"))
