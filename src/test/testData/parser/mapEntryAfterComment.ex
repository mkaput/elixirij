%{
  name: "default",
  # keep a comment between entries
  files: %{
    included: ["lib/"],
    excluded: [~r"/_build/"]
  },
  plugins: []
}
