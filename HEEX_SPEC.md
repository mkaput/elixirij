# HEEx Support Specification

This document defines the architecture and implementation plan for adding HEEx (HTML-aware EEx) support to Elixirij.
The goal is to match the practical baseline implemented by IntelliJ Elixir and then build a clean foundation for
future features (completion, navigation, inspections) without shipping those yet.

## Goals

1. Recognize `.heex` files and provide basic IDE experience (icons, file type, syntax highlighting).
2. Parse HEEx as a template language with HTML as the template-data language.
3. Support HEEx-specific constructs:
    - `{...}` expression interpolation in text and attribute values
    - `<% %>`, `<%= %>`, `<%# %>` EEx tags
    - component tags `<.component>` and slot tags `<:slot>`
4. Preserve HTML features (structure view, tag matching, CSS/JS injection, HTML inspections) where possible.
5. Avoid HTML inspection noise for valid HEEx constructs (e.g., invalid tag name for `<.component>`).
6. Ensure Elixir parsing/highlighting works inside HEEx expressions via language injection.

## Non-goals (v1)

- Full semantic validation of HEEx (e.g., LiveView-specific validations).
- Type checking or component prop/slot validation.
- Advanced formatter or refactoring in HEEx files.
- Precise AST equivalence with Phoenix compiler output.

## Baseline parity with IntelliJ Elixir (reference behavior)

We target at least the following behaviors observed in IntelliJ Elixir's HEEx support:

- `.heex` file type recognition + icon
- Syntax highlighting for `{...}` and EEx tags
- Component tag support for `<.component>` (and slot tags as a follow-up)
- Suppression of HTML “invalid tag name” inspection for HEEx component tags
- CSS/JS language injection inside `<style>` and `<script>` blocks

## Reference: Phoenix LiveView HEEx implementation

The canonical HEEx implementation lives in the Phoenix LiveView repository and is used for `.heex` templates and
the `~H` sigil. These modules are the reference for expected behavior:

- `Phoenix.LiveView.HTMLEngine` (`lib/phoenix_live_view/html_engine.ex`)
    - Entry point for `.heex` compilation; calls `EEx.compile_string/2` with `Phoenix.LiveView.TagEngine` as the engine.
    - Implements `Phoenix.LiveView.TagEngine` behavior to classify tags:
        - `":" <> name` → slot tags (`<:slot>`)
        - `"." <> name` → local components (`<.component>`)
        - leading uppercase → remote components (`<My.Component>`)
        - `":" <> "inner_block"` is reserved and errors
        - HTML tags fallback to `{:tag, name}`
    - Defines HTML void tags via `void?/1`.

- `Phoenix.LiveView.TagEngine` (`lib/phoenix_live_view/tag_engine.ex`)
    - An `EEx.Engine` implementation; EEx handles `<% %>` tokenization.
    - Uses `Phoenix.LiveView.Tokenizer` to tokenize HTML + `{...}` interpolations.
    - Delegates tag classification to the configured tag handler (`HTMLEngine`).

- `Phoenix.LiveView.Tokenizer` (`lib/phoenix_live_view/tokenizer.ex`)
    - Custom tokenizer for HTML + HEEx curly interpolation.
    - Tracks contexts `:text`, `:script`, `:style`, and HTML comments.
    - Curly interpolation `{...}` is enabled in `:text` and in attribute values.
    - Braces are disabled inside `<script>`/`<style>` and can be disabled per-tag via
      `phx-no-curly-interpolation` attribute.
    - Curly parsing supports nested braces and escaped braces (`\\{`, `\\}`).
    - EEx interpolation inside tag attribute values is rejected; HEEx expects `{expr}` syntax.

We should strive for maximal compatibility with Phoenix behavior (especially tag classification and curly rules),
while using IntelliJ’s template-language facilities and maintaining IDE-grade error tolerance and recovery.

## Compatibility & error tolerance

- Mirror Phoenix semantics wherever feasible (token boundaries, tag classification, curly rules, slot/component tags).
- Prefer IDE-friendly error recovery over strict failures: never throw on malformed templates; produce PSI with
  localized error nodes and keep HTML/Elixir injection alive when possible.

## High-level architecture

HEEx will be implemented as a **template language** with HTML as the template-data language.
The HEEx language is responsible for recognizing template markers, while HTML handles the structure of the
surrounding markup.

### Package layout

All HEEx-specific code lives under `dev.murek.elixirij.heex`. Integration points from other packages should only
reference HEEx types via public APIs (file type, language, parser definition, injectors, etc.).

Key components:

- `HeexLanguage` (TemplateLanguage)
- `HeexFileType` (extension `heex`)
- `HeexParserDefinition`
- `HeexFile` (PSI file for HEEx outer language)
- `HeexTemplateDataElementType` (HTML template data)
- `HeexFileViewProvider` (maps HEEx + HTML PSI)
- `HeexLexer` (outer language lexer)
- `HeexParser` / `Heex.bnf` (minimal grammar for template blocks)
- `HeexSyntaxHighlighter` + `HeexHtmlSyntaxHighlighter`
- `HeexLanguageInjector` (inject ExLanguage into HEEx expressions)
- `HeexHtmlTagNameLexer` (wrap HTML lexer to accept `<.component>`)
- `HeexHtmlInspectionSuppressor` (suppresses invalid tag name inspection)

## File type and registration

### New file type

- `HeexFileType` in `dev.murek.elixirij.heex`.
- Extension: `heex`.
- Display name: "HEEx".
- Description: "Elixir HEEx template".
- Icon: new HEEx icon (see Resources section), with a dark variant.

### Plugin registration

Add to `src/main/resources/META-INF/plugin.xml`:

- `<fileType>` for HEEx file type.
- `<lang.parserDefinition>` for `HeexLanguage`.
- `<lang.syntaxHighlighterFactory>` for HEEx.
- `<lang.commenter>` for HEEx (optional v1, see below).
- `<lang.braceMatcher>` for HEEx (optional v1 but recommended for `{}` and `<% %>`).
- `<lang.fileViewProviderFactory>` if needed (TemplateLanguageFileViewProvider).
- `<lang.syntaxHighlighterFactory>` for `HTML` is not touched; we add a custom HTML highlighter specifically for
  HEEx view providers (see below).

### LSP integration

Decision: HEEx files should be treated as Elixir for LSP startup. This keeps language tooling available (completion,
references) in HEEx expressions.

- Extend `FileType.isElixir` to include `HeexFileType` or add a new property `isElixirLike` and update
  `ExLspServerSupportProvider` to use it.

## Template language model

### Template data language

- Use `HTMLLanguage` (platform HTML parser) as the template data language.
- Create `HeexTemplateDataElementType` (TemplateDataElementType) with:
    - `language = HeexLanguage`
    - `templateDataLanguage = HTMLLanguage.INSTANCE`
    - `outerElementType = HEEX_OUTER_LANGUAGE` (token type for template markers)
    - `templateTextTokenType = HEEX_TEMPLATE_TEXT` (token type for HTML segments)

### View provider

- Implement `HeexFileViewProvider` extending `TemplateLanguageFileViewProvider`.
- Provide both HEEx and HTML PSI roots:
    - HEEx PSI uses `HeexParserDefinition`.
    - HTML PSI uses `HTMLLanguage` and its default parser.
- `getTemplateDataLanguage()` returns HTML.
- Ensure outer language ranges are excluded from the HTML PSI via `HeexOuterLanguageRangePatcher`.

## Lexer and parser

### Token model (outer language)

Define HEEx token types in a new `HeexElementTypes.kt` under `dev.murek.elixirij.heex` (or grouped in a local
`ElementTypes.kt` if consistent with HEEx-only layout). Minimal tokens:

- `HEEX_TEMPLATE_TEXT` (HTML fragment sent to HTML parser)
- `HEEX_EXPR_START` (`{`)
- `HEEX_EXPR_END` (`}`)
- `HEEX_EEX_OPEN` (`<%`)
- `HEEX_EEX_OUTPUT` (`<%=`)
- `HEEX_EEX_COMMENT_OPEN` (`<%#`)
- `HEEX_EEX_CLOSE` (`%>`)
- `HEEX_EEX_BODY` (EEx body between open/close)
- `HEEX_EXPR_BODY` (expression body between `{` and `}`)
- `HEEX_BAD_CHARACTER`

### Lexer strategy

Implement `HeexLexer` in Kotlin (`LexerBase`) rather than JFlex, because:

- It must track nested delimiters inside `{...}` to avoid terminating on inner `}`.
- It must disable `{...}` recognition inside `<script>` and `<style>` blocks.
- It must recognize `<%`, `<%=`, `<%#`, and `<%%` patterns accurately without leaking into HTML parsing.

State machine:

1. **DATA** (default)
    - Emit `HEEX_TEMPLATE_TEXT` until a template marker is found.
    - Markers: `{`, `<%`, `<%=`, `<%#`.
    - If current position is inside `<script>` or `<style>`, do **not** treat `{` as HEEx start.

2. **BRACE_EXPR**
    - Emit `HEEX_EXPR_START`, then `HEEX_EXPR_BODY`, then `HEEX_EXPR_END`.
    - Use a nested counter for `{}` while scanning `HEEX_EXPR_BODY`.
    - Ignore braces inside Elixir strings/heredocs/charlists; track those by delegating to `ExLexer` or lightweight
      string-state tracking.
    - Fail-safe: if EOF before matching `}`, emit `HEEX_EXPR_BODY` to EOF and mark `HEEX_EXPR_END` as missing
      (parser can recover).

3. **EEX_BLOCK**
    - Emit open token (`HEEX_EEX_OPEN`, `HEEX_EEX_OUTPUT`, or `HEEX_EEX_COMMENT_OPEN`), then `HEEX_EEX_BODY`, then
      `HEEX_EEX_CLOSE`.
    - Do not attempt to parse Elixir here; injection will handle it.

### Parser (outer language)

Create a minimal Grammar-Kit parser for HEEx in `src/main/grammars/Heex.bnf`.
The grammar only needs to build PSI nodes for template elements and enable language injection.

Example structure:

```
heexFile ::= item*
item ::= templateText
       | braceExpr
       | eexExpr
       | eexOutput
       | eexComment

templateText ::= HEEX_TEMPLATE_TEXT

braceExpr ::= HEEX_EXPR_START exprBody? HEEX_EXPR_END
exprBody ::= HEEX_EXPR_BODY

// EEx
// <%= %> is output, <% %> is non-output, <%# %> is comment
// The body is kept as a single token for injection

eexExpr ::= HEEX_EEX_OPEN eexBody? HEEX_EEX_CLOSE

eexOutput ::= HEEX_EEX_OUTPUT eexBody? HEEX_EEX_CLOSE

eexComment ::= HEEX_EEX_COMMENT_OPEN eexBody? HEEX_EEX_CLOSE

eexBody ::= HEEX_EEX_BODY
```

### PSI elements

- `HeexTemplateText` (for `HEEX_TEMPLATE_TEXT`)
- `HeexBraceExpression`
- `HeexEexExpression`
- `HeexEexOutput`
- `HeexEexComment`
- `HeexExpressionBody` and `HeexEexBody` should implement `PsiLanguageInjectionHost`.

## HTML integration and component tags

### Component tags `<.component>`

HTML/XML lexers treat tag names starting with `.` as invalid. To keep HTML parsing and highlighting intact,
implement a lexer wrapper for HTML:

- `HeexHtmlTagNameLexer` wraps the HTML lexer.
- It substitutes the leading `.` (and optionally `:` for slots) with a valid character (e.g., `C`) **in the input
  used by the lexer only** (via a `CharSequence` wrapper). Offsets remain unchanged.
- This makes `<.component>` lex as a normal tag while preserving original offsets for PSI and highlighting.

The wrapper is used only for the HTML template-data parser/highlighter of HEEx files (not globally).

### Slot tags `<:slot>`

HEEx slot tags use `:` as a prefix. HTML may accept `:` in tag names but XML constraints vary. To be safe,
apply the same substitution approach as with `.`:

- Detect `<:` and `</:` in the lexer wrapper.
- Substitute `:` for a valid character during HTML lexing.

### HTML inspection suppression

Add a dedicated HTML inspection suppressor for HEEx files:

- Implement `XmlSuppressor` or `HtmlLocalInspectionTool` suppressor for `XmlInvalidTagNameInspection` and
  `XmlUnknownTagInspection` in HEEx files.
- Suppress only when the tag name starts with `.` or `:`.

## Syntax highlighting

### HEEx highlighter (outer language)

`HeexSyntaxHighlighter` assigns:

- `HEEX_EEX_*` and `HEEX_EXPR_*` tokens → new HEEx attributes or reuse Elixir attributes.
- `HEEX_EEX_COMMENT_OPEN` and `HEEX_EEX_BODY` → comment color.
- `HEEX_EXPR_BODY` and `HEEX_EEX_BODY` → delegate to injected Elixir highlighting (not direct styling).

Implementation guidance (based on IntelliJ template languages):

- Reuse HTML highlighting wherever possible by delegating to `HtmlFileHighlighter` and only overriding HEEx-specific
  tokens. This preserves HTML tag/attribute styling out-of-the-box.
- This matches patterns in IntelliJ template languages such as Angular (`Angular2HtmlFileHighlighter`),
  Vue (`VueFileHighlighter`), and Astro (`AstroFileHighlighter`), all of which extend or wrap `HtmlFileHighlighter`
  and only add template-specific token mappings.

New text attributes (if needed):

- `HEEX_TEMPLATE_TAG` (for `<% %>`)
- `HEEX_INTERPOLATION_BRACE` (for `{}`)

### HTML highlighter in HEEx

Implement `HeexHtmlSyntaxHighlighter` that uses the HTML lexer wrapper to recognize component tags as
regular tags. Register it only for HEEx view providers. It should still reuse HTML attribute/tag keys
from `HtmlFileHighlighter` (e.g., `XmlHighlighterColors.HTML_ATTRIBUTE_NAME`) and only override
HEEx-specific tokens.

## Language injection

Use `MultiHostInjector` to inject Elixir language into HEEx expression bodies:

- Injection targets: `HeexExpressionBody` and `HeexEexBody` PSI elements.
- Inject `ExLanguage` with `Prefix/Suffix` as needed (usually none).
- For `<%# %>`, do not inject.
- For `HEEX_EXPR_BODY`, inject only when not inside `<script>` or `<style>` sections; rely on lexer to avoid creating
  such nodes in those ranges.

## Editor helpers

Optional v1 (small, low risk):

- `HeexBraceMatcher` for `{}` and `<% %>` pairs.
- `HeexQuoteHandler` if auto-completion of quotes inside `{}` is needed.

## Resources and UI

- Add HEEx icons to `src/main/resources/icons`:
    - `heex.svg`
    - `heex_dark.svg`
- Add messages to `src/main/resources/messages/ExBundle.properties`:
    - `filetype.heex.description=HEEx template file`
    - `filetype.heex.name=HEEx`

## Test plan

### Lexer tests (Kotlin)

- `test heex brace interpolation` – `{ @user.name }` is tokenized as expr start/body/end.
- `test heex nested braces` – `{ %{a: %{b: 1}} }` does not terminate early.
- `test heex eex output` – `<%= @x %>` emits output-open/body/close.
- `test heex eex comment` – `<%# ignored %>` is comment token.
- `test heex no brace in script` – `{}` inside `<script>` is treated as template text.

### Parser tests

- `test heex file with mixed items` – HTML + `{}` + `<% %>` produces correct PSI item sequence.
- `test heex unterminated brace` – parser recovers at EOF with error node.

### HTML integration tests

- `test heex component tag is parsed as html tag` – `<.card>` appears in HTML PSI without invalid tag errors.
- `test heex slot tag parsed` – `<:header>` parsed as HTML tag.
- `test html inspections suppressed for dot/slot tags` – no invalid tag inspection.

### Injection tests

- `test elixir injection in braces` – `{@name}` is injected with Elixir language.
- `test elixir injection in eex` – `<%= @name %>` is injected with Elixir language.
- `test no injection in script/style` – braces inside `<script>` have no Elixir injection.

### LSP integration tests (optional)

- Ensure opening a `.heex` file starts LSP server (if enabled), using file type predicate.

## Phased delivery

Each phase is intentionally small and should include its tests. Mark completed phases with a trailing
`DONE ✅` sentence. No phases are marked `DONE ✅` yet.

1. **Phase 1: HEEx language + file type**
    - Add `HeexLanguage`, `HeexFileType`, bundle strings, icons, and plugin.xml registration.
    - File type association: `.heex` is recognized in IDE.
    - Tests:
        - file type registration test (extension -> `HeexFileType`).
        - icon presence sanity (resource lookup).

2. **Phase 2: Template data wiring**
    - Add `HeexTemplateDataElementType` + `HeexFileViewProvider`.
    - Ensure HTML is the template data language and outer language ranges are excluded from HTML PSI.
    - Tests:
        - view provider test: `getTemplateDataLanguage()` is HTML.
        - HTML PSI exists for `.heex` file.

3. **Phase 3: Minimal HEEx lexer (template markers only)**
    - Implement `HeexLexer` that recognizes `<% %>` and `{}` markers and emits template text.
    - No nested brace handling yet; just correct token boundaries for simple cases.
    - Tests:
        - lexer: `<%= @x %>` token sequence.
        - lexer: `{@x}` token sequence.

4. **Phase 4: Minimal HEEx grammar + PSI**
    - Add `Heex.bnf` + parser definition + PSI types.
    - Parse template text and the basic brace/EEx nodes.
    - Tests:
        - parser: mixed HTML + `{}` + `<% %>` yields expected PSI items.
        - parser: unterminated `<%` recovers with error node.

5. **Phase 5: Brace nesting + escaping**
    - Extend lexer to support nested `{}` and escaped braces `\\{`/`\\}`.
    - Tests:
        - lexer: nested brace interpolation `{ %{a: %{b: 1}} }`.
        - lexer: escaped braces stay inside body.

6. **Phase 6: Script/style brace suppression**
    - Disable `{}` interpolation inside `<script>` and `<style>`.
    - Tests:
        - lexer: `{}` inside script is template text.
        - lexer: `{}` inside style is template text.

7. **Phase 7: EEx comment handling**
    - Ensure `<%# %>` is handled as HEEx comment tokens.
    - Tests:
        - lexer: `<%# ignored %>` yields comment token(s).
        - parser: comment node exists and does not inject.

8. **Phase 8: Elixir injection (brace expressions)**
    - Inject `ExLanguage` into `{...}` bodies.
    - Tests:
        - injection: `{@name}` produces injected Elixir PSI.

9. **Phase 9: Elixir injection (EEx expressions)**
    - Inject `ExLanguage` into `<% %>` and `<%= %>` bodies.
    - Tests:
        - injection: `<%= @name %>` produces injected Elixir PSI.
        - no injection: `<%# %>` comment.

10. **Phase 10: HTML lexer wrapper for component tags**
    - Wrap HTML lexer for HEEx view providers to accept `<.component>` and `<:slot>`.
    - Tests:
        - HTML PSI: `<.card>` parsed as HTML tag.
        - HTML PSI: `<:header>` parsed as HTML tag.

11. **Phase 11: HTML inspection suppression**
    - Suppress invalid-tag inspections for `<.component>` and `<:slot>` only.
    - Tests:
        - inspection: no invalid tag warnings for dot/slot tags.

12. **Phase 12: HEEx syntax highlighter**
    - Provide HEEx syntax highlighter that delegates to `HtmlFileHighlighter` and only adds HEEx tokens.
    - Tests:
        - highlighting: `{}` / `<% %>` delimiters get HEEx-specific keys.
        - highlighting: HTML attribute names use HTML attribute keys.

13. **Phase 13: HTML syntax highlighter for HEEx view providers**
    - Provide HEEx-specific HTML highlighter wrapper using the tag-name lexer wrapper.
    - Tests:
        - highlighting: `<.component>` tag name uses HTML tag name color (not error).

14. **Phase 14: LSP integration**
    - Treat `.heex` as Elixir-like for LSP startup.
    - Tests:
        - file-open hook includes `.heex` in LSP start predicate.

15. **Phase 15: Editor helpers (optional)**
    - Brace matcher and quote handler for HEEx.
    - Tests:
        - typing test for `{}` or `<% %>` pair completion (fixture-based).

16. **Phase 16: Polishing + regression tests**
    - Add regression tests for key Phoenix behaviors (component tags, slots, curly rules).
    - Expand edge-case parser recovery tests.

## Open questions / decisions

1. **Lexer implementation**: Kotlin lexer vs JFlex. Kotlin is preferred for flexible brace handling.
2. **Slot tag handling**: confirm whether `<:slot>` needs lexer substitution on the IntelliJ XML parser.
3. **Injection boundaries**: decide whether `<%` bodies should allow nested `%>` in strings (likely no).
4. **LSP inclusion**: verify LSP server supports `.heex` before auto-starting in these files.

## Notes on platform conventions

- Use light services only when required (e.g., injection registration is a project-level extension, not a service).
- Use Kotlin coroutines for any background work (no heavy blocking).
- Prefer expression functions for simple getters.
