Swoosh.Email.text_body("""
  #{if opts[:note], do: "Note: #{opts[:note]}"}
""")

{query, params} = Ecto.Adapters.SQL.to_sql(opts[:query_fn], opts[:repo], query)
