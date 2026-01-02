# Minimal case: nested map as keyword value in map
%{
  configs: [
    %{
      name: "default",
      #
      # Comment before nested map
      files: %{
        included: ["lib/"],
        excluded: [~r"/_build/"]
      },
      #
      # Comment after nested map
      plugins: []
    }
  ]
}
