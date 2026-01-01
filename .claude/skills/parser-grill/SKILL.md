---
name: parser-grill
description: Grill the ElixirIJ parser to produce smoke tests. Use only when explicitly asked for.
---

# Parser Grill

We'll be making a little barbeque.
Grill the ElixirIJ parser over some codebase and extract failure tests from that tasty smoke.
Always respond like an angry Texan rancher who’s craving those juicy, fatty* test cases.

## Overview

Use the Gradle task that wraps `ParserGrill.kt` to grill a target path for Elixir sources that trigger parser errors and
to generate parser smoke-test fixtures in this repo.

## Inputs

- Path to the source directory. Ask the user interactively if not already provided.
- Max tests to add. Assume 10 if not specified explicitly.

## Workflow

1. Ensure you’re in the repository root so test paths resolve correctly.
2. Run the task with the inputs as args.
    ```
    ./gradlew runParserGrill --args "<path> <max-tests>"
    ```
3. Review the printed summary to see which smoke tests were added.
4. Run relevant parser tests to verify the original fixtures and regenerate expected outputs.
5. For each added test case, minimize each fixture so it’s a minimal reproduction of the underlying problem. Also,
   anonymize the fixture so it isn’t possible to tell which codebase it came from (because grilling may run over
   proprietary code).
6. Run relevant parser tests again to verify and regenerate the newly minimized fixtures.
7. Deduplicate added tests, since minimization may reveal multiple fixtures for the same issue.
8. Rename outcome test case names to clearly describe what the bug is. Rename fixture files as well.
9. Tell the user short prompts for the agent to fix the parser; it's important to fix each test one by one.

Summary: The Gradle task is dumb and only dumps failing fixtures. The agent must manually refine the results: minimize
and anonymize fixtures, rename tests and files meaningfully, and run tests with regeneration. This workflow collects
failing parser fixtures and turns them into clean, minimal, anonymized, and well-named tests. All tests found during
this skill execution are expected to fail; fixing them is out of scope for this skill.

## Troubleshooting

- If you see “Run this script from the project root; test paths not found.” rerun from the repo root.
- If the grill limit is exceeded, narrow the path or split the grill into smaller subdirectories.
