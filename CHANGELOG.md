<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Elixirij Changelog

## [Unreleased]

### Changed

- Release metadata bump to `0.0.2` (no functional changes).

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
