# Environment Setup for ElixirIJ Plugin Development

This file provides step-by-step instructions for setting up the ElixirIJ development environment. Follow these steps when working with GitHub Copilot Coding Agent.

## Prerequisites

1. **Java Development Kit (JDK) 21**
   - The project requires JDK 21 as specified in the build configuration
   - Verify installation: `java -version` should show version 21.x.x
   - Download from: https://adoptium.net/ (Eclipse Temurin recommended)

2. **Gradle 9.2.1** (automatically handled by Gradle wrapper)
   - No manual installation required
   - The project includes `./gradlew` wrapper script that downloads the correct version

## Initial Setup

1. **Clone and Navigate to Repository**
   ```bash
   cd /home/runner/work/elixirij/elixirij
   ```

2. **Verify Gradle Wrapper**
   ```bash
   ./gradlew --version
   ```
   This should download Gradle 9.2.1 (if not already present) and display version info.

## Build System

This is an IntelliJ Platform Plugin project built with Gradle. Key technologies:
- **Language**: Kotlin (JVM target 21)
- **Build Tool**: Gradle with IntelliJ Platform Gradle Plugin
- **Target Platform**: IntelliJ IDEA 2025.2.5+
- **Lexer Generator**: JFlex via Grammar-Kit plugin

## Essential Build Tasks

1. **Generate Lexer** (required before compilation)
   ```bash
   ./gradlew generateElixirLexer
   ```
   Generates the Elixir lexer from `src/main/grammars/Elixir.flex`

2. **Build the Plugin**
   ```bash
   ./gradlew build
   ```
   Compiles code, generates lexer, runs tests, and creates plugin distribution

3. **Run Tests**
   ```bash
   ./gradlew test
   ```
   Executes all unit and integration tests

4. **Run IDE with Plugin**
   ```bash
   ./gradlew runIde
   ```
   Launches IntelliJ IDEA with the plugin installed for manual testing

5. **Clean Build Artifacts**
   ```bash
   ./gradlew clean
   ```
   Removes all generated files and build outputs

## Code Coverage

- Generate HTML coverage report:
  ```bash
  ./gradlew koverHtmlReport
  ```
  Report available at: `build/reports/kover/html/index.html`

## Development Workflow

1. **Before Making Changes**
   - Ensure lexer is generated: `./gradlew generateElixirLexer`
   - Run existing tests to establish baseline: `./gradlew test`

2. **After Making Changes**
   - Rebuild: `./gradlew build`
   - Run tests: `./gradlew test`
   - Verify plugin loads: `./gradlew runIde` (if needed)

3. **For Lexer/Grammar Changes**
   - Edit `src/main/grammars/Elixir.flex`
   - Regenerate lexer: `./gradlew generateElixirLexer`
   - Rebuild and test

## Project Structure

- **Source Code**: `src/main/kotlin/dev/murek/elixirij/`
- **Tests**: `src/test/kotlin/dev/murek/elixirij/`
- **Grammars**: `src/main/grammars/`
- **Resources**: `src/main/resources/`
- **Build Output**: `build/` (excluded from Git)

## Code Style Guidelines

1. Use modern Kotlin coroutines for threading (IntelliJ Platform's coroutine model)
2. Prefer expression functions for simple cases: `fun f() = ...`
3. Test naming convention: `` fun `test something is happening`() { ... } ``
4. Follow existing patterns in the codebase

## Common Issues

- **Lexer not found errors**: Run `./gradlew generateElixirLexer` first
- **JDK version mismatch**: Ensure JDK 21 is active
- **Gradle daemon issues**: Try `./gradlew --stop` then retry the command
- **Configuration cache issues**: Run `./gradlew --no-configuration-cache <task>` if needed

## Additional Resources

- Plugin Configuration: `gradle.properties`
- Build Script: `build.gradle.kts`
- Coding Guidelines: `AGENTS.md`
- IntelliJ Platform Docs: https://plugins.jetbrains.com/docs/intellij/
- Kotlin Coroutines in IntelliJ: https://plugins.jetbrains.com/docs/intellij/kotlin-coroutines.html
