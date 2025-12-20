# ElixirIJ

This project implements a new from-scratch Elixir language support for all IntelliJ Platform-based IDEs.
We target the latest and greatest platform version and modern Elixir language conventions.

## Code rules

1. Use modern Kotlin coroutines-powered threading model of IntelliJ Platform.
   Docs: https://plugins.jetbrains.com/docs/intellij/kotlin-coroutines.html
2. Prefer expression functions (`fun f() = ...`) instead of statement ones (`fun f() { ... }`) for simple cases.
3. Try to write tests for every change, unless you're introducing a trivial "declarative" feature (like code commenter
   configuration).
4. Name tests like this, unless base class requires classical camelCase test naming:
    ```kotlin
    fun `test something is happening`() { ... }
    ```

## Tools

```bash
# Build whole project
./gradlew buildPlugin

# Run all tests
./gradlew check

# Run single test
./gradlew test --tests "dev.murek.elixirij.lang.ExLexerTest.testIntegerLiterals"

# Format code
# This project uses .editorconfig for code style conventions.
# Format code using your IDE's auto-format feature (Ctrl+Alt+L in IntelliJ).
# The build will use the configured EditorConfig settings automatically.

# Run inspections
./gradlew qodanaScan

# Extract IntelliJ Platform sources
# Use this when you need to read through IntelliJ Platform sources
# Extracts sources to .refs/intellij-platform and prints the path
./bin/intellij-platform-src

# Clone Elixir language source repository
# Use this when you need to read through Elixir source code
# Clones elixir-lang/elixir to .refs/elixir and prints the path
./bin/elixir-src

# Clone ElixirLS source repository
# Use this when you need to read through ElixirLS source code
# Clones elixir-lsp/elixir-ls to .refs/elixir-ls and prints the path
./bin/expert-src

# Clone IntelliJ-Elixir source repository
# Use this when you need to read through IntelliJ-Elixir source code
# Clones KronicDeth/intellij-elixir to .refs/intellij-elixir and prints the path
./bin/intellij-elixir-src
```
