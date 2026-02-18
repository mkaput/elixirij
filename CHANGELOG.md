<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Elixirij Changelog

## [Unreleased]

### Added

- Automatic exclusion of Mix-generated and dependency directories (`_build`, `deps`, `.elixir_ls`, `.expert`) for detected Mix roots, including nested/umbrella roots.

## [0.0.2] - 2026-02-04

**Full Changelog**: https://github.com/mkaput/elixirij/compare/v0.0.1...0.0.2

## [0.0.1] - 2026-02-04

A new project has seen the light of day!

### Added

- Elixir language registration with `.ex` and `.exs` file types (with icons).
- Lexer, parser, and PSI scaffolding for modern Elixir syntax.
- Syntax highlighting (lexical + semantic), color settings page, and bundled color schemes.
- Brace/quote matching plus smart typing/backspace handling for paired delimiters and interpolation.
- Code folding for blocks, containers, strings, sigils, and heredocs.
- Commenter support.
- Escape sequence validation annotations.
- `mix format` integration via the configured Elixir toolchain.
- Toolchain detection and settings UI with validation/test button.
- Expert LSP integration with auto-download/update and configurable mode/custom path.
- Optional spellchecking strategy with bundled dictionary (when the platform spellchecker is enabled).
- Initial scaffold created from
  [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)

[Unreleased]: https://github.com/mkaput/elixirij/compare/v0.0.2...HEAD

[0.0.2]: https://github.com/mkaput/elixirij/compare/v0.0.1...v0.0.2

[0.0.1]: https://github.com/mkaput/elixirij/commits/v0.0.1
