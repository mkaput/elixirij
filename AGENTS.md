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
