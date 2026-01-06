# Elixir String Interpolation Specification

This document defines how Elixir string interpolation (`#{...}`) is lexed, parsed, highlighted, and tested in ElixirIJ.

It follows the Python f-string implementation pattern in IntelliJ (Python.flex + PyLexerFStringHelper +
ExpressionParsing)
by using explicit lexer states and dedicated interpolation tokens, rather than late injection.

## Scope

Support every interpolation form valid in Elixir:

- Double-quoted strings: `"...#{expr}..."`
- Heredocs: `"""...#{expr}..."""`
- Sigils with interpolation enabled (lowercase sigils such as `~s`, `~r`, `~w`, `~c`, `~S` vs `~s` handling)
- Deprecated interpolation in single-quoted charlists: `'#\{expr}'` and `'''...#\{expr}...'''`
- Escapes: `\#{` is not an interpolation start
- Nested braces and nested strings inside interpolation expressions

Non-goals:

- Non-standard or removed syntax that is not accepted by current Elixir

Deprecated behavior to support:

- Interpolation inside single-quoted charlists (`'...'`) and `'''...'''` should be parsed and tokenized,
  even though Elixir emits a deprecation warning and suggests migrating to `~c""`.

## Syntax summary

Interpolation is `#{` followed by a full Elixir expression, closed by the matching `}`.

Examples:

- `"Hello #{name}"`
- `"#{inspect(%{a: 1})}"`
- `"#{ "b" <> "#{a}" }"`
- `~s(Hello #{name})`
- `"""
  Hello #{name}
  """`

## Lexer design (Python-style)

Model after Python f-string support:

- Python uses explicit FSTRING states in the lexer, with a helper that tracks brace balance
  and returns `FSTRING_FRAGMENT_START/END`. We use the same technique for Elixir.

### Lexer states

At minimum:

- `IN_STRING`
- `IN_HEREDOC`
- `IN_SIGIL_INTERP` (sigils that allow interpolation)
- `IN_INTERPOLATION` (expression mode inside `#{...}`)

Optional additional sigil states depending on current lexer structure (delimiter-specific).

### Tokens

Use ElixirIJ token naming conventions (EX_*):

- `EX_INTERPOLATION_START` for `#{`
- `EX_INTERPOLATION_END` for the matching `}`
- `EX_STRING_PART` for plain content within an interpolated string
- `EX_STRING_BEGIN` / `EX_STRING_END` (if not already represented)

### Behavior

- While in `IN_STRING` / `IN_HEREDOC` / `IN_SIGIL_INTERP`:
    - Emit `EX_STRING_PART` until an unescaped `#{` or string end.
    - `\#{` is treated as literal text.
- On unescaped `#{`:
    - Emit `EX_INTERPOLATION_START`, switch to `IN_INTERPOLATION`.
- In `IN_INTERPOLATION`:
    - Lex normal Elixir tokens.
    - Track brace balance for `{` and `}`.
    - When balance returns to 0 and `}` is read, emit `EX_INTERPOLATION_END` and return to the string state.
- Nested interpolations inside interpolations are allowed by brace tracking and normal lexing.

### Sigils

- Lowercase sigils enable interpolation (e.g., `~s`, `~r`, `~w`, `~c`), uppercase disable it.
- When interpolation is disabled, `#{` must be treated as literal text.

## Parser design

Model after Python's `parseFormattedStringNode` and `parseFStringFragment`:

```
interpolated_string
  ::= EX_STRING_BEGIN (EX_STRING_PART | interpolation)* EX_STRING_END

interpolation
  ::= EX_INTERPOLATION_START expression EX_INTERPOLATION_END
```

Heredocs and interpolated sigils mirror `interpolated_string`.

### Error recovery

- If interpolation is not closed, recover at string end or line break.
- Keep parsing the string, but emit error nodes for incomplete interpolation.

## PSI model

Introduce PSI nodes (or adapt existing ones):

- `ExInterpolatedString`
- `ExStringPart`
- `ExInterpolation` with child expression

Helpers:

- `isInterpolated()` on string/sigil PSI
- `interpolations()` and `textParts()`

## Highlighting and braces

- Highlight `EX_STRING_PART` as string.
- Highlight `EX_INTERPOLATION_START` / `EX_INTERPOLATION_END` using brace or interpolation style.
- Ensure brace matcher recognizes interpolation braces only inside interpolated strings/sigils.

## Editor behavior

- Typing `#{` inside an interpolated string/sigil should insert `#{}` and place caret inside.
- Backspace should remove paired `}` when appropriate.

## Tests

Required per project rules.

### Lexer tests

- `"a #{x} b"`
- `"a \\#{x} b"` (no interpolation)
- nested braces: `"#{%{a: {b: 1}}}"`
- heredoc: `"""#{x}\n"""`
- sigils: `~s(#{x})` vs `~S(#{x})`

### Parser/PSI tests

- `interpolated_string` tree shape and `ExInterpolation` nodes
- unclosed interpolation recovery

### Editor tests

- typing `#{` inserts `#{}` and caret is inside
- full document checks using fixture `checkResult` style

## References

- Python f-strings (IntelliJ):
    - Lexer states and tokens: `python/python-parser/src/com/jetbrains/python/lexer/Python.flex`
    - State helper and brace tracking: `python/python-parser/src/com/jetbrains/python/lexer/PyLexerFStringHelper.kt`
    - Parser integration: `python/python-parser/src/com/jetbrains/python/parsing/ExpressionParsing.java`

- Groovy GStrings (IntelliJ):
    - `plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/lexer/groovy.flex`
    - `plugins/groovy/groovy-psi/src/org/jetbrains/plugins/groovy/lang/parser/groovy.bnf`

- Dart string templates (dart-intellij-third-party):
    - `third_party/src/main/java/com/jetbrains/lang/dart/lexer/Dart.flex`
    - `third_party/gen/com/jetbrains/lang/dart/DartParser.java`

## Phases

Each phase must be implemented with its own tests and committed separately.
After implementing a phase, update the corresponding phase header to include `DONE ✅`.

### Phase 1: Token and lexer state scaffolding — DONE ✅

Goal: introduce interpolation tokens and lexer states without changing parser behavior.

Work:

- Define `EX_INTERPOLATION_START`, `EX_INTERPOLATION_END`, `EX_STRING_PART` (and `EX_STRING_BEGIN/END` if needed).
- Add lexer states for interpolated strings and interpolation expressions.
- Tokenize `#{` inside interpolated strings as `EX_INTERPOLATION_START` and return to string state after
  `EX_INTERPOLATION_END`.
- Keep parser unchanged for now (tokens may appear but not parsed as structure).

Tests:

- Lexer tests for `EX_INTERPOLATION_START/END` and `EX_STRING_PART`:
    - `"a #{x} b"`
    - `"a \\#{x} b"`
    - `"""#{x}\n"""`
    - `~s(#{x})` and `~S(#{x})`

Commit: add tests and lexer changes.

### Phase 2: Parser support for interpolated strings — DONE ✅

Goal: parse interpolations as part of string PSI.

Work:

- Extend grammar with `interpolated_string` and `interpolation` rules using EX_* tokens.
- Create PSI nodes (`ExInterpolatedString`, `ExInterpolation`, `ExStringPart`) or adapt existing ones.
- Ensure expressions inside interpolation reuse existing expression parser.

Tests:

- Parser/PSI tests for interpolated strings:
    - `"#{inspect(%{a: 1})}"`
    - `"#{ "b" <> "#{a}" }"`
    - `~s(#{x})`
- Unclosed interpolation recovery tests.

Commit: add grammar/PSI changes + parser tests.

### Phase 3: Deprecated charlist interpolation — DONE ✅

Goal: support deprecated interpolation in single-quoted charlists.

Work:

- Allow interpolation tokens and parsing inside `'...'` and `'''...'''` when `#{` is present.
- Ensure warning-level diagnostic can be added later (not required in this phase).

Tests:

- Lexer and parser tests:
    - `'#\{a}'` should parse with interpolation.
    - `'''\n#{a}\n'''` should parse with interpolation.

Commit: add charlist interpolation support + tests.

### Phase 4: Highlighting and brace matching — DONE ✅

Goal: highlight interpolation delimiters and handle brace matching correctly.

Work:

- Highlight `EX_INTERPOLATION_START/END` distinctly.
- Update brace matcher to pair interpolation braces only inside interpolated strings.

Tests:

- Highlighting tests for interpolation braces in strings and sigils.
- Brace matching test (if applicable in test framework).

Commit: add highlighting/brace matcher tests + implementation.

### Phase 5: Editor typing behavior

Goal: improve typing UX in interpolated strings.

Work:

- Add typed handler to auto-insert `#{}` in interpolated contexts.
- Backspace handling for paired `}`.

Tests:

- Fixture tests:
    - Typing `#{` inserts `#{}` and caret inside.
    - Backspace removes paired `}` when appropriate.

Commit: add editor handler tests + implementation.
