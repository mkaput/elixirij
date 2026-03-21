# Elixirij

A new from-scratch Elixir language support plugin for all IntelliJ Platform-based IDEs.

## Rules

- you may be running in parallel with other agents; cooperate to avoid conflicts, but avoid commiting changes made by
  others
- write tests for every change, unless introducing "declarative" features (code commenter etc.)
- when bugfixing: write minimal reproduction test that fails, then fix, then confirm test passes
- remember to run tests for related changes
- don’t mock IntelliJ Platform internals; prefer real platform test fixtures and pipelines instead
- ensure any feature/bugfix work is covered in @CHANGELOG.md

## Code rules

- target the latest IntelliJ Platform version (2026.01+) and modern Elixir language conventions (1.20+)
- use light services and Kotlin coroutines-powered threading model of IntelliJ Platform
- prefer expression functions (`fun f() = ...`) instead of statement ones (`fun f() { ... }`) for simple cases
- name tests like this, unless base class (fixtures) requires testCamelCase:
    ```kotlin
    fun `test something is happening`() { ... }
    ```
- prefer `parent / child` instead of `parent.resolve(child)` when glueing `java.nio.file.Path`s together
- when writing fixture tests, always prefer comparing whole actual and expected fixture states:

  Good example:
  ```kotlin
  fun `test double quote typing`() {
      myFixture.configureByText("test.ex", "x = <caret>")
      myFixture.type('"')
      myFixture.checkResult("x = \"<caret>\"")
  }
  ```

  Bad example:
  ```kotlin
  fun `test double quote typing`() {
      myFixture.configureByText("test.ex", "x = <caret>")
      myFixture.type('"')
      assertTrue("Should have paired quotes", myFixture.editor.document.text.contains("\"<caret>\""))
  }
  ```

## Tools

```bash
# Build whole project
./gradlew buildPlugin

# Run all tests
./gradlew check

# Run single test
./gradlew test --tests "dev.murek.elixirij.lang.ExLexerTest.testIntegerLiterals"

# Regenerate test fixtures
# Use this to regenerate expected output files for ParsingTestCase and similar tests
JAVA_TOOL_OPTIONS="-Didea.tests.overwrite.data=true" ./gradlew test --tests "dev.murek.elixirij.lang.parser.ExParserTest"

# Format code
# This project uses .editorconfig for code style conventions.
# Format code using your IDE's auto-format feature (Ctrl+Alt+L in IntelliJ).
# The build will use the configured EditorConfig settings automatically.
```
