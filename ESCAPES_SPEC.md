# Elixir Escape Sequences Specification

This document defines how Elixir escape sequences are lexed, highlighted, validated, and used by editor features in ElixirIJ.
It is scoped to IntelliJ Platform lexer/highlighter integration and Elixir's current escape rules.

## Scope

- Strings: "..." and heredocs """...""".
- Charlists: '...' and heredocs '''...''' (deprecated but still tokenized).
- Interpolating sigils (~[a-z]) where escapes are currently parsed alongside interpolation.
- Syntax highlighting for valid and invalid escape sequences.
- Spell checking that skips escape sequences.

Non-goals:

- Regex-specific escapes (e.g., ~r) are not interpreted as Elixir string escapes.
- Raw sigils (~[A-Z]) remain a single token and are not parsed for escapes.
- Runtime semantics or full evaluation of sigils.

## Reference sources (ref-src)

### Elixir core

- `lib/elixir/src/elixir_interpolation.erl`
  - `unescape_chars/3`, `unescape_hex/3`, `unescape_unicode/3`, `unescape_map/1` define the accepted escape sequences.
- `lib/elixir/src/elixir_tokenizer.erl`
  - Tokenizer error cases for invalid escapes and end-of-file handling.

### IntelliJ Platform (Java)

- `com/intellij/lexer/StringLiteralLexer.java`
  - Emits `StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN` and invalid escape tokens.
  - Supports optional hex (`\x..`), octal, and Unicode escapes.
  - Allows optional escaped EOL or framing spaces (used by Java text blocks).
  - Extension hook for additional escape sequence forms.
- `com/intellij/lexer/JavaStringLiteralLexer.java`
  - Java-specific Unicode handling with multiple leading `u` (e.g., `\uuuu0041`).
  - Handles `\u` sequences that evaluate to backslash and then continue parsing escapes.
  - Special-case to avoid marking `\{` as invalid inside string template fragments.

### Expert (ElixirLS fork)

- `apps/forge/src/future_elixir_interpolation.erl`
  - `unescape_hex/3` accepts deprecated `\xH` and `\x{H*}` forms and emits warnings.
  - `unescape_unicode/3` supports `\uHHHH` and `\u{H*}`.
  - `unescape_map/1` matches Elixir core for simple escapes.
- `apps/forge/src/future_elixir_tokenizer.erl`
  - Tokenizer error messages for invalid escapes and EOF handling.

### IntelliJ-Elixir

- `src/org/elixir_lang/Elixir.flex`
  - Lexer states `ESCAPE_SEQUENCE`, `HEXADECIMAL_ESCAPE_SEQUENCE`, `UNICODE_ESCAPE_SEQUENCE`,
    and `EXTENDED_HEXADECIMAL_ESCAPE_SEQUENCE` tokenize escapes inside groups (strings, heredocs, sigils).
  - Supports `\x{H*}` and `\u{H*}` with 1–6 hex digits via `EXTENDED_HEXADECIMAL_ESCAPE_SEQUENCE`.
  - Allows escaping terminators in literal sigils (even when interpolation is disabled).
- `src/org/elixir_lang/annotator/EscapeSequence.kt`
  - Highlights valid escape sequences only.
- `src/org/elixir_lang/spell_checking/literal/Splitter.kt`
  - Excludes escape sequences with a regex-based splitter before spell checking.

## Elixir escape sequences (source of truth: `elixir_interpolation.erl`)

### Simple escapes

These are unescaped via `unescape_map/1`:

- `\0` (null byte)
- `\a` (alert)
- `\b` (backspace)
- `\d` (delete)
- `\e` (escape)
- `\f` (form feed)
- `\n` (newline)
- `\r` (carriage return)
- `\s` (space)
- `\t` (tab)
- `\v` (vertical tab)
- `\\` (backslash)
- `\"` (double quote, in strings)
- `\'` (single quote, in charlists)

### Generic escapes

Elixir allows backslash to escape any character. For characters not listed above, the backslash is dropped and the
escaped character is used as-is.

### Hex escapes

- `\xHH` where H is a hex digit (exactly two digits).

### Unicode escapes

- `\uHHHH` where H is a hex digit (exactly four digits).
- `\u{H...}` with 1 to 6 hex digits inside braces.

Unicode code points must be valid UTF-8 scalars. Invalid or reserved code points are errors.

### Escaped newlines

- `\` followed by `\n` or `\r\n` is treated as a line continuation (newline removed).

### Invalid escapes

Invalid escapes reported by Elixir core include:

- `\x` not followed by two hex digits.
- `\u` not followed by four hex digits or a valid brace form.
- `\u{}` with zero digits or more than six digits.
- Invalid Unicode code points in `\u{...}`.
- `\` at end of file (or unclosed string/heredoc).

### Legacy/deprecated hex forms (compatibility note)

Expert and IntelliJ-Elixir accept deprecated hex escapes:

- `\xH` (single hex digit)
- `\x{H...}` with 1–6 hex digits

Elixir core treats these as deprecated or invalid depending on version. For ElixirIJ, we will support them for
compatibility and highlight them as valid escapes (see Phase 2).

## Where escapes are recognized

- Double-quoted strings and heredocs.
- Single-quoted charlists and charlist heredocs (deprecated but still supported).
- Interpolating sigils (~[a-z]) use the same lexer states as strings today; escapes are currently indistinguishable
  from regex or word-list escape semantics. See phases for how we handle this.

Raw sigils (~[A-Z]) are tokenized as a single `EX_SIGIL` token and do not participate in escape lexing.

## Lexer design

### Tokens

Prefer IntelliJ standard escape tokens for interoperability:

- `StringEscapesTokenTypes.VALID_STRING_ESCAPE_TOKEN`
- `StringEscapesTokenTypes.INVALID_CHARACTER_ESCAPE_TOKEN`
- `StringEscapesTokenTypes.INVALID_UNICODE_ESCAPE_TOKEN`

If custom tokens are required for Elixir-specific cases, keep them in a dedicated `EX_STRING_ESCAPES` token set and map
them to the same highlighter attributes.

### Regex macros (Elixir.flex)

Define macros for reuse across string states:

- `HEX_DIGIT = [0-9a-fA-F]`
- `UNICODE_SHORT = "\\u" {HEX_DIGIT}{4}`
- `UNICODE_BRACED = "\\u{" {HEX_DIGIT}{1,6} "}"`
- `HEX_ESC = "\\x" {HEX_DIGIT}{2}`
- `SIMPLE_ESC = "\\" [0abdefnrstv\\\"']`
- `ESCAPED_NEWLINE = "\\\n" | "\\\r\n"`
- `GENERIC_ESC = "\\" .`

Order matters: invalid hex/unicode patterns must be matched before the generic escape rule.

### Interpolation safety

`\#{` must not start interpolation. Consuming `\#` as an escape ensures the following `{` is treated as plain text.

### Error granularity

Tokenize invalid escapes as a single token spanning the backslash and the invalid sequence prefix (e.g., `\xZ`, `\u{`)
so highlighting is localized.

## Highlighting

- Valid escapes: `DefaultLanguageHighlighterColors.VALID_STRING_ESCAPE`.
- Invalid escapes: `DefaultLanguageHighlighterColors.INVALID_STRING_ESCAPE`.

Add `ExTextAttributes.STRING_ESCAPE` and `ExTextAttributes.INVALID_STRING_ESCAPE` and map them in
`ExSyntaxHighlighter.getTokenHighlights`.

## Spellchecker behavior (issue 53)

Spell checking should operate on the unescaped string content, with proper offset mapping from the original PSI.
Use `EscapeSequenceTokenizer.processTextWithOffsets` as in IntelliJ Groovy spellchecking.

## Tests

### Lexer tests

Add lexer tests under `src/test/testData/lexer` and `ExLexerTest`:

- Valid escapes: `"\\n"`, `"\\t"`, `"\\xFF"`, `"\\u00A9"`, `"\\u{1F600}"`, `"\\\n"`.
- Generic escape: `"\\q"` (should be treated as a valid escape in Elixir).
- Invalid escapes: `"\\xZ"`, `"\\u12"`, `"\\u{}"`, `"\\u{1234567}"`.
- Interpolation escape: `"\\#{not_interpolated}"`.
- Charlists: `'\\n'`, `'\\xFF'`, invalid variants.
- Sigils: `~s(\\n)` should show escape tokens; `~S(\\n)` should not.

### Highlighting tests (optional)

If we add highlighting tests, ensure escape tokens are mapped to the new attributes.

### Spellchecker tests

Add tests verifying that words adjacent to escapes are tokenized correctly and escapes do not appear in spellcheck input.

## Phases

Each phase is an atomic commit. After implementation, mark the phase as DONE in this file.

### Phase 1: Escape tokens and highlighting for standard strings (DONE)

Work:

- Add escape tokens (or reuse `StringEscapesTokenTypes`).
- Add text attributes for valid/invalid string escapes.
- Update `ExSyntaxHighlighter` to map escape tokens to these attributes.
- Update the color settings demo text to include escape sequences.
- Extend `Elixir.flex` to tokenize escape sequences inside `IN_STRING` and `IN_HEREDOC` only.

Tests:

- New lexer test file: `escapes_strings.ex`.
- Verify token stream includes escape tokens for valid and invalid escapes.

Commit message:

- `add escape sequence tokens and highlighting for strings`

### Phase 2: Charlist, sigil, and deprecated escape support (DONE)

Work:

- Add escape tokenization for `IN_CHARLIST` and `IN_CHARLIST_HEREDOC`.
- Track sigil letter to limit escape highlighting to `~s` and `~c` (and their heredoc forms).
  - If tracking is deferred, document that all interpolating sigils will highlight escapes as a temporary behavior.
- Ensure `~S` and `~C` remain raw (no escape tokens).
- Add support for deprecated `\xH` and `\x{H*}` escapes (treated as valid with compatibility highlighting).

Tests:

- Lexer test file: `escapes_charlists_and_sigils.ex`.
- Verify `~s`/`~c` token streams include escape tokens and `~S`/`~C` do not.
- Add lexer cases for deprecated `\xH` and `\x{H*}` in strings and charlists.

Commit message:

- `tokenize escapes in charlists and string sigils`

### Phase 3: Invalid escape diagnostics (DONE)

Work:

- Add annotator checks for invalid escape tokens.
- For `\u{...}`, validate code point range and mark as invalid when out of range or reserved.
- Surface errors with `HighlightSeverity.ERROR` and a clear message.

Tests:

- Annotator tests for invalid hex/unicode escapes and invalid code points.

Commit message:

- `report invalid escape sequences in strings`

### Phase 4: Spellchecker integration (issue 53)

Work:

- Implement `EscapeSequenceTokenizer` usage in `ExSpellcheckingStrategy`.
- Use the same escape rules as the lexer for consistent offsets.

Tests:

- Spellchecker tests ensuring escaped sequences are skipped and words are checked correctly.

Commit message:

- `improve spellchecker escape handling fix #53`
