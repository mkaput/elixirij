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

## Reference: Elixir EEx implementation

EEx is part of Elixir itself and defines the core tokenization and engine semantics that HEEx builds upon.
These modules are the canonical reference for tag syntax, token boundaries, and engine callbacks:

- `EEx` (`lib/eex/lib/eex.ex`)
    - Documents the supported tags:
        - `<% %>` (execute, discard output), `<%= %>` (execute, output),
          `<%%` (escape), and `<%!-- --%>` (comment, discarded).
        - `<%#` is deprecated in favor of `<%!--` (warning emitted).
    - Default engine is `EEx.SmartEngine`.

- `EEx.Compiler` (`lib/eex/lib/eex/compiler.ex`)
    - Tokenizes the template into `{:text, ...}`, `{:expr, ...}`, and block
      variants `{:start_expr | :middle_expr | :end_expr, ...}`.
    - Recognizes markers immediately after `<%`: `""`, `"="`, `"/"`, `"|"`.
      Markers `"/"` and `"|"` are reserved for custom engines.
    - Implements `<%!-- --%>` multi-line comments and `<%%` escaping.

- `EEx.Engine` and `EEx.SmartEngine` (`lib/eex/lib/eex/engine.ex`, `lib/eex/lib/eex/smart_engine.ex`)
    - `EEx.Engine` defines callbacks for template text and expressions, and
      restricts marker behavior.
    - `EEx.SmartEngine` adds `@assign` handling on top of the base engine.

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

## Reference: IntelliJ Elixir (intellij-elixir) implementation

IntelliJ Elixir implements EEx/LEEx as a template language with HTML (or other file types) as the template-data
language. HEEx support is mentioned in the v22.0.0 changelog, but there is no dedicated `heex` package or `.heex`
file type entry in this snapshot; the existing EEx template stack is the closest concrete reference.

Key pieces worth mirroring or adapting:

- **Template language wiring**
    - `org.elixir_lang.eex.file.ViewProvider` is a `MultiplePsiFilesPerDocumentFileViewProvider` and
      `ConfigurableTemplateLanguageFileViewProvider` that chooses the template-data language from
      `TemplateDataLanguageMappings` or by stripping the last extension (e.g., `*.html.eex` → HTML), then falls
      back to `Language.defaultTemplateLanguageFileType()`.
    - The view provider includes `EEx`, template-data language, and `ElixirLanguage` in `getLanguages()`, and uses a
      custom `EmbeddedElixir` element type when parsing Elixir inside template data.

- **Template data element type**
    - `org.elixir_lang.eex.element_type.TemplateData` extends `TemplateDataElementType` using an outer
      `OuterLanguageElementType` (`TemplateData.EEX`) and token type `Types.DATA`, with a base lexer of
      `org.elixir_lang.eex.lexer.TemplateData`.
    - `TemplateData` lexer merges all EEx tag tokens into a single outer token, keeping non-template text as
      `DATA`, which is then lexed by the template-data language.

- **EEx lexer model**
    - `EEx.flex` defines `<% %>` tokenization with marker states (`#`, `=`, `/`, `|`) and a separate comment state.
    - `LookAhead` wraps the Flex lexer and merges `DATA`, `ELIXIR`, and `COMMENT` tokens for stable outer-language
      boundaries.

- **Highlighter layering**
    - `org.elixir_lang.eex.TemplateHighlighter` is a layered editor highlighter that uses:
        - the template-data language syntax highlighter for `Types.DATA`,
        - the Elixir highlighter for `Types.ELIXIR`,
        - and an EEx-specific lexer for template tags.

- **File types + registration**
    - `org.elixir_lang.eex.file.Type` is a `TemplateLanguageFileType` (extension `eex`), and
      `org.elixir_lang.leex.file.Type` extends it for `leex`.
    - `resources/META-INF/plugin.xml` registers the EEx language parser definition and file view provider factory
      for EEx/LEEx.

- **Inline HEEx via sigil injection**
    - `org.elixir_lang.injection.ElixirSigilInjector` injects `HTMLLanguage` into `~H` sigils (and `EEx` into `~L`)
      using `MultiHostInjector`. This is an opt-in experimental feature documented in `README.md`.

This reference suggests a pragmatic approach: use IntelliJ’s template-language infrastructure (view provider +
template data element type + layered highlighters) as the baseline, then layer HEEx-specific lexing, tag handling,
and inspection suppression on top.

## Reference tests: IntelliJ Elixir (EEx / HEEx)

This is the definitive list of EEx/HEEx-related tests and fixtures present in the IntelliJ Elixir snapshot used
by this spec. Use these as a behavioral source of truth and mirror them where applicable.

### EEx lexer tests (LookAhead)

Located under `tests/org/elixir_lang/eex/lexer/look_ahead`:

All three tests assert exact token sequences and lexer state transitions (Flex states).
For each variant below, the **input** is the literal string shown and the **expected output** is the ordered
token sequence with the lexer state after each `advance()`:

**BodylessTest** (parameterized by single tag variant)

- Input: `<%#%>`
  Expected tokens:
    - `<%` → `OPENING`, next state `MARKER_MAYBE`
    - `#` → `COMMENT_MARKER`, next state `COMMENT`
    - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`
- Input: `<%%%>` (escaped opening followed by close)
  Expected tokens:
    - `<%%` → `ESCAPED_OPENING`, next state `YYINITIAL`
    - `%>` → `DATA`, next state `YYINITIAL`
- Input: `<%/%>`
  Expected tokens:
    - `<%` → `OPENING`, next state `MARKER_MAYBE`
    - `/` → `FORWARD_SLASH_MARKER`, next state `ELIXIR`
    - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`
- Input: `<%=%>`
  Expected tokens:
    - `<%` → `OPENING`, next state `MARKER_MAYBE`
    - `=` → `EQUALS_MARKER`, next state `ELIXIR`
    - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`
- Input: `<%|%>`
  Expected tokens:
    - `<%` → `OPENING`, next state `MARKER_MAYBE`
    - `|` → `PIPE_MARKER`, next state `ELIXIR`
    - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`

**WhiteSpaceBodyTest** (same variants, body is a single space)

- Input: `<% %>`
  Expected tokens:
    - `<%` → `OPENING`, next state `MARKER_MAYBE`
    - `` (empty) → `EMPTY_MARKER`, next state `ELIXIR`
    - ` ` → `ELIXIR`, next state `ELIXIR`
    - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`
- Input: `<%# %>`
  Expected tokens:
    - `<%` → `OPENING`, next state `MARKER_MAYBE`
    - `#` → `COMMENT_MARKER`, next state `COMMENT`
    - ` ` → `COMMENT`, next state `COMMENT`
    - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`
- Input: `<%% %>`
  Expected tokens:
    - `<%%` → `ESCAPED_OPENING`, next state `YYINITIAL`
    - ` %>` → `DATA`, next state `YYINITIAL`
- Input: `<%/ %>`, `<%= %>`, `<%| %>`
  Expected tokens:
    - `<%` → `OPENING`, next state `MARKER_MAYBE`
    - `/` or `=` or `|` → marker token, next state `ELIXIR`
    - ` ` → `ELIXIR`, next state `ELIXIR`
    - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`

**MinimalBodyTest** (parameterized by **pairs** of tag variants)

- Tag variants (single tag, body is `"body"`):
    - `<%body%>`
        - `<%` → `OPENING`, next state `MARKER_MAYBE`
        - `` (empty) → `EMPTY_MARKER`, next state `ELIXIR`
        - `body` → `ELIXIR`, next state `ELIXIR`
        - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`
    - `<%#body%>`
        - `<%` → `OPENING`, next state `MARKER_MAYBE`
        - `#` → `COMMENT_MARKER`, next state `COMMENT`
        - `body` → `COMMENT`, next state `COMMENT`
        - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`
    - `<%%body%>` (escaped opening; `body%>` is a single `DATA` token)
        - `<%%` → `ESCAPED_OPENING`, next state `YYINITIAL`
        - `body%>` → `DATA`, next state `YYINITIAL`
    - `<%/body%>`
        - `<%` → `OPENING`, next state `MARKER_MAYBE`
        - `/` → `FORWARD_SLASH_MARKER`, next state `ELIXIR`
        - `body` → `ELIXIR`, next state `ELIXIR`
        - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`
    - `<%=body%>`
        - `<%` → `OPENING`, next state `MARKER_MAYBE`
        - `=` → `EQUALS_MARKER`, next state `ELIXIR`
        - `body` → `ELIXIR`, next state `ELIXIR`
        - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`
    - `<%|body%>`
        - `<%` → `OPENING`, next state `MARKER_MAYBE`
        - `|` → `PIPE_MARKER`, next state `ELIXIR`
        - `body` → `ELIXIR`, next state `ELIXIR`
        - `%>` → `CLOSING`, next state `WHITESPACE_MAYBE`
- Input: concatenation of any two variants above (36 combinations).
  Expected output: exact concatenation of both token sequences (no over-consumption across the boundary).

### EEx parser fixture corpus (non-test inputs)

Located under `testData/org/elixir_lang/eex/psi/parser/test` (fixtures only in this snapshot):

These are **inputs** only; the IntelliJ Elixir snapshot does not include explicit expected outputs for them.
If we adopt them, we should capture and lock PSI tree snapshots from our implementation to define expected output.

- `DoEExClosingWhiteSpaceEExOpening.eex`
- `EExTemplate.eex`
- `EExTemplateWithBindings.eex`
- `EExTokenizerTestComments.eex`
- `EExTokenizerTestCommentsWithDoEnd.eex`
- `EExTokenizerTestQuotation.eex`
- `EExTokenizerTestQuotationWithDoEnd.eex`
- `EExTokenizerTestQuotationWithInterpolation1.eex`
- `EExTokenizerTestQuotationWithInterpolation2.eex`
- `EExTokenizerTestStringWithEmbeddedCode.eex`
- `EExTokenizerTestStringsWithEmbeddedDoEnd.eex`
- `EExTokenizerTestStringsWithEmbeddedKeywordsBlocks.eex`
- `EExTokenizerTestStringsWithEmbeddedStabOperatorEnd.eex`
- `EExTokenizerTestStringsWithMoreThanOneLine.eex`
- `EExTokenizerTestStringsWithMoreThanOneLineAndExpressionWithMoreThanOneLine.eex`
- `EExTokenizerTestTrimMode.eex`
- `EExTokenizerTestTrimModeSetToFalse.eex`
- `EExTokenizerTestTrimModeWithCRLF.eex`
- `EExTokenizerTestTrimModeWithComment.eex`
- `Fn1Clause.eex`
- `Fn1ClauseCall.eex`
- `Fn2ClauseCall.eex`
- `FnEExClosing.eex`
- `FnEExElixirEExStabBody.eex`
- `FnEExElixirStabBody.eex`
- `FnElixirEExElixirStabBody.eex`
- `FnElixirEExStabBody.eex`
- `FnUnexpectedEnd.eex`
- `PhoenixTemplatesLayoutApp.html.eex`
- `PhoenixTemplatesPageIndex.html.eex`
- `PhoenixTemplatesUserEdit.html.eex`
- `PhoenixTemplatesUserForm.html.eex`
- `PhoenixTemplatesUserIndex.html.eex`
- `PhoenixTemplatesUserNew.html.eex`
- `PhoenixTemplatesUserShow.html.eex`
- `StringSample.eex`

### EEx completion test

- `tests/org/elixir_lang/code_insight/completion/contributor/CallDefinitionClauseTest`
    - `testExecuteOnEExFunctionFrom` uses fixtures `eex_function.ex` and `eex.ex`.
    - Input:
        - `eex_function.ex` contains:
            - `EEx.function_from_file(:def, :function_from_file_sample, "sample.eex", [:a, :b])`
            - `EEx.function_from_string(:def, :function_from_string_sample, "<%= a + b %>", [:a, :b])`
            - Completion caret at `f<caret>` inside `def usage do`.
        - `eex.ex` provides the `EEx` module definition.
    - Expected output:
        - Completion list contains `function_from_file_sample` and `function_from_string_sample` only.
        - Each lookup element tail text is exactly ` (eex_function.ex defmodule EExFunction)`.

### EEx parsing coverage in Elixir stdlib

- `tests/org/elixir_lang/parser_definition/ElixirLangElixirParsingTestCase`
    - Input → expected output (`Parse.CORRECT` = no error elements and quoting succeeds):
        - `lib/eex/lib/eex.ex` → `Parse.CORRECT`
        - `lib/eex/lib/eex/compiler.ex` → `Parse.CORRECT`
        - `lib/eex/lib/eex/engine.ex` → `Parse.CORRECT`
        - `lib/eex/lib/eex/smart_engine.ex` → `Parse.CORRECT`
        - `lib/eex/lib/eex/tokenizer.ex` → `Parse.CORRECT`
        - `lib/mix/lib/mix/tasks/compile.leex.ex` → `Parse.CORRECT` (LEEx-related but adjacent)

### Decompiler regression with EEx module

- `tests/org/elixir_lang/beam/DecompilerTest`
    - Input: module name `Elixir.EExTestWeb.PageController`.
    - Expected output: decompiled text matches
      `testData/org/elixir_lang/beam/decompiler/Elixir.EExTestWeb.PageController.ex` exactly.

### HEEx tests

No HEEx-specific tests or `.heex` fixtures were found in this IntelliJ Elixir snapshot (no `*heex*` test classes
and no `.heex` files under `testData`).

## EEx vs HEEx differences (behavioral outline)

This section is meant to keep the two template systems distinct while maximizing shared infrastructure.

- **Template model**
    - EEx is a general-purpose templating system that treats the surrounding content as opaque text
      (or a template-data language chosen by file extension).
    - HEEx is HTML-aware and relies on HTML tokenization and tag structure for correctness and IDE features.

- **Interpolation syntax**
    - EEx: `<% %>` / `<%= %>` tags are the primary embedding mechanism; `<%%` escapes.
    - HEEx: supports EEx tags *and* `{...}` interpolation in text and attributes; curly interpolation is
      disabled in `<script>`/`<style>` and can be disabled per-tag.

- **Comments**
    - EEx: canonical comment form is `<%!-- --%>`; `<%#` is deprecated.
    - HEEx: uses EEx comment tags for template comments; keep `<%# %>` support for parity with existing tooling.

- **Tag semantics**
    - EEx has no concept of HTML tags or components.
    - HEEx classifies tags (HTML, components, slots, remote components) via `Phoenix.LiveView.TagEngine`.

- **Expression handling**
    - EEx engines interpret markers (`""`, `"="`, `"/"`, `"|"`) and handle assigns via `EEx.SmartEngine`.
    - HEEx delegates code handling to the tag engine and HTML tokenizer and must enforce HEEx-specific
      constraints (e.g., no EEx interpolation in attribute values; `{expr}` instead).

## Shared foundation: EEx + HEEx

Much of the template-language scaffolding can be shared. Implementing EEx first provides:

- the template language file type and view provider pattern,
- template-data element type and outer-language token boundaries,
- layered syntax highlighting approach,
- EEx lexer/parser model for `<% %>` tags.

HEEx then extends this with HTML-specific tokenization, curly interpolation rules, tag-name adjustments,
and inspection suppression for components/slots.

### Shared components mapped to phases

- **EEx Phase 1 (language + file type)** → reused by HEEx Phase 7 (HEEx language + file type).
- **EEx Phase 2 (template data wiring + view provider)** → HEEx Phase 8 adapts this with HTML as fixed
  template data language and HEEx-specific outer element types.
- **EEx Phase 3 (lexer + parser for `<% %>` tags)** → HEEx Phase 9/10 extend this with `{}` interpolation
  and HEEx PSI nodes.
- **EEx Phase 4 (layered highlighter)** → HEEx Phase 17 keeps the layered approach and adds HEEx tokens.
- **EEx Phase 5 (Elixir injection for `<% %>` bodies)** → HEEx Phase 14 reuses the injector and adds `{}` bodies.

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
- It substitutes the leading `.` with a valid character (e.g., `C`) **in the input
  used by the lexer only** (via a `CharSequence` wrapper). Offsets remain unchanged.
- This makes `<.component>` lex as a normal tag while preserving original offsets for PSI and highlighting.

The wrapper is used only for the HTML template-data parser/highlighter of HEEx files (not globally).

### Slot tags `<:slot>`

HEEx slot tags use `:` as a prefix. IntelliJ’s HTML/XML lexers already accept `:` in tag names
(`TAG_NAME`/`NAME` patterns include `:` at the start), and `XmlUtil.isValidTagNameChar` treats `:` as valid.
Therefore **no lexer substitution is required** for `:`. We should still suppress unknown-tag inspections for
slot tags, but we don’t need to rewrite tag names for parsing/highlighting.

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

### EEx template tests

- `test eex file type registration` – `.eex` resolves to `EexFileType`.
- `test eex template data language from extension` – `foo.html.eex` uses HTML as template data language.

### EEx lexer tests (Kotlin)

- `test eex output tag` – `<%= @x %>` emits output-open/body/close.
- `test eex comment tag` – `<%# ignored %>` yields comment token(s).
- `test eex escaped opening` – `<%%` is tokenized as escaped opening.

### EEx parser tests

- `test eex file with mixed items` – template text + `<% %>` produces expected PSI items.

### EEx injection tests

- `test elixir injection in eex` – `<%= @name %>` is injected with Elixir language.

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

1. **Phase 1: EEx language + file type**
    - Add `EexLanguage`, `EexFileType`, bundle strings, icons, and plugin.xml registration.
    - File type association: `.eex` is recognized in IDE.
    - Tests:
        - file type registration test (extension -> `EexFileType`).
        - icon presence sanity (resource lookup).

2. **Phase 2: EEx template data wiring**
    - Add `EexTemplateDataElementType` + `EexFileViewProvider`.
    - Select template data language by stripping the last extension (`*.html.eex` → HTML), then fall back to
      `TemplateDataLanguageMappings` and `Language.defaultTemplateLanguageFileType()`.
    - Tests:
        - view provider test: `foo.html.eex` uses HTML template data language.
        - template data PSI exists for `.eex` file.

3. **Phase 3: EEx lexer + minimal grammar**
    - Implement `EexLexer` for `<% %>`, `<%= %>`, `<%# %>`, `<%%`, and `<%!-- --%>` (comment).
    - Add `Eex.bnf` + parser definition + PSI types for EEx tags.
    - Tests:
        - lexer: `<%= @x %>` token sequence.
        - lexer: `<%%` escaped opening token.
        - parser: mixed template text + `<% %>` yields expected PSI items.

4. **Phase 4: EEx syntax highlighter**
    - Provide layered syntax highlighting with template-data language + Elixir.
    - Tests:
        - highlighting: EEx tag delimiters get EEx-specific keys.
        - highlighting: template data tokens use underlying language keys.

5. **Phase 5: EEx injection**
    - Inject `ExLanguage` into EEx body nodes (`<% %>`, `<%= %>`).
    - Tests:
        - injection: `<%= @name %>` produces injected Elixir PSI.
        - no injection: comment tag.

6. **Phase 6: EEx editor helpers (optional)**
    - Brace matcher for `<% %>` and quote/typing helpers if needed.

7. **Phase 7: HEEx language + file type**
    - Add `HeexLanguage`, `HeexFileType`, bundle strings, icons, and plugin.xml registration.
    - File type association: `.heex` is recognized in IDE.
    - Tests:
        - file type registration test (extension -> `HeexFileType`).
        - icon presence sanity (resource lookup).

8. **Phase 8: HEEx template data wiring**
    - Add `HeexTemplateDataElementType` + `HeexFileViewProvider`.
    - Ensure HTML is the template data language and outer language ranges are excluded from HTML PSI.
    - Tests:
        - view provider test: `getTemplateDataLanguage()` is HTML.
        - HTML PSI exists for `.heex` file.

9. **Phase 9: HEEx lexer (template markers only)**
    - Extend `EexLexer` with `{}` recognition for HEEx and emit template text accordingly.
    - No nested brace handling yet; just correct token boundaries for simple cases.
    - Tests:
        - lexer: `<%= @x %>` token sequence.
        - lexer: `{@x}` token sequence.

10. **Phase 10: HEEx grammar + PSI**
    - Add `Heex.bnf` + parser definition + PSI types.
    - Parse template text and the basic brace/EEx nodes.
    - Tests:
        - parser: mixed HTML + `{}` + `<% %>` yields expected PSI items.
        - parser: unterminated `<%` recovers with error node.

11. **Phase 11: Brace nesting + escaping**
    - Extend lexer to support nested `{}` and escaped braces `\\{`/`\\}`.
    - Tests:
        - lexer: nested brace interpolation `{ %{a: %{b: 1}} }`.
        - lexer: escaped braces stay inside body.

12. **Phase 12: Script/style brace suppression**
    - Disable `{}` interpolation inside `<script>` and `<style>`.
    - Tests:
        - lexer: `{}` inside script is template text.
        - lexer: `{}` inside style is template text.

13. **Phase 13: EEx comment handling in HEEx**
    - Ensure `<%# %>` and `<%!-- --%>` are handled as HEEx comment tokens.
    - Tests:
        - lexer: `<%# ignored %>` yields comment token(s).
        - parser: comment node exists and does not inject.

14. **Phase 14: Elixir injection (HEEx braces + EEx expressions)**
    - Inject `ExLanguage` into `{...}` bodies and `<% %>`/`<%= %>` bodies.
    - Tests:
        - injection: `{@name}` produces injected Elixir PSI.
        - injection: `<%= @name %>` produces injected Elixir PSI.
        - no injection: comment tags.

15. **Phase 15: HTML lexer wrapper for component tags**
    - Wrap HTML lexer for HEEx view providers to accept `<.component>` and `<:slot>`.
    - Tests:
        - HTML PSI: `<.card>` parsed as HTML tag.
        - HTML PSI: `<:header>` parsed as HTML tag.

16. **Phase 16: HTML inspection suppression**
    - Suppress invalid-tag inspections for `<.component>` and `<:slot>` only.
    - Tests:
        - inspection: no invalid tag warnings for dot/slot tags.

17. **Phase 17: HEEx syntax highlighter**
    - Provide HEEx syntax highlighter that delegates to `HtmlFileHighlighter` and only adds HEEx tokens.
    - Tests:
        - highlighting: `{}` / `<% %>` delimiters get HEEx-specific keys.
        - highlighting: HTML attribute names use HTML attribute keys.

18. **Phase 18: HTML syntax highlighter for HEEx view providers**
    - Provide HEEx-specific HTML highlighter wrapper using the tag-name lexer wrapper.
    - Tests:
        - highlighting: `<.component>` tag name uses HTML tag name color (not error).

19. **Phase 19: LSP integration**
    - Treat `.heex` as Elixir-like for LSP startup.
    - Tests:
        - file-open hook includes `.heex` in LSP start predicate.

20. **Phase 20: HEEx editor helpers (optional)**
    - Brace matcher and quote handler for HEEx.
    - Tests:
        - typing test for `{}` or `<% %>` pair completion (fixture-based).

21. **Phase 21: Polishing + regression tests**
    - Add regression tests for key Phoenix behaviors (component tags, slots, curly rules).
    - Expand edge-case parser recovery tests.

## Decisions and confirmations

1. **Lexer implementation**: Kotlin lexer (preferred for nested `{}` and context tracking).
2. **Slot tag handling**: `<:slot>` does **not** need lexer substitution; `:` is accepted by IntelliJ HTML/XML
   lexers and tag-name validation.
3. **Injection boundaries**: EEx closes on the first `%>` even inside strings. Verified by
   `elixir -e 'EEx.compile_string("<%= \"%>\" %>")'` which fails with an unterminated string error.
4. **LSP inclusion**: Expert supports `.heex` (component navigation works); allow LSP auto-start for HEEx. Assume same
   happens for EEx.
