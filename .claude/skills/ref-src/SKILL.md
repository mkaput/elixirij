---
name: ref-src
description: >-
    Check out reference codebase sources (IntelliJ Platform, Elixir, Expert, IntelliJ-Elixir, IntelliJ Plugins).
    Use when asked to fetch/extract any of these sources.
---

When asked to consult the codebase of one of the following projects, run the associated script.
The script will print a full path to the codebase checkout which you can then browse through.

| Project           | Script                                          |
|-------------------|-------------------------------------------------|
| IntelliJ Platform | `<path-to-skill>/scripts/intellij-platform-src` |
| Elixir            | `<path-to-skill>/scripts/elixir-src`            |
| Expert            | `<path-to-skill>/scripts/expert-src`            |
| IntelliJ-Elixir   | `<path-to-skill>/scripts/intellij-elixir-src`   |
| IntelliJ Plugins  | `<path-to-skill>/scripts/intellij-plugins-src`  |

## Example

```bash
$ scripts/intellij-platform-src
/some/path/to/intellij-community
```
