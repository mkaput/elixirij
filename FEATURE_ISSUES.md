# Feature Implementation Issues

This document contains detailed GitHub issue descriptions for features that can be implemented with the current incomplete parser.

---

## Issue 1: Implement code folding for Elixir structures

### Description

Add code folding support for Elixir data structures and code blocks to improve code readability and navigation in large files.

### Features to Implement

#### Foldable Structures
- **Lists**: `[...]`
- **Maps**: `%{...}`
- **Tuples**: `{...}`
- **Bitstrings**: `<<...>>`
- **Heredocs**: `"""..."""` and `'''...'''`
- **Multi-line sigils**: `~r/.../`, `~s/.../`, etc.
- **Future**: do-blocks when parser supports them

#### User Experience
- Placeholders should show structure type (e.g., `[...]`, `%{...}`)
- Nested structures should fold independently
- Fold by default for large structures (configurable)

### IntelliJ Platform Extension Points

#### 1. `com.intellij.lang.foldingBuilder`

**Implementation**: Create `ExFoldingBuilder` implementing `FoldingBuilder` or `FoldingBuilderEx`

```kotlin
class ExFoldingBuilder : FoldingBuilderEx() {
    override fun buildFoldRegions(
        root: PsiElement,
        document: Document,
        quick: Boolean
    ): Array<FoldingDescriptor>

    override fun getPlaceholderText(node: ASTNode): String?

    override fun isCollapsedByDefault(node: ASTNode): Boolean
}
```

**Register in** `plugin.xml`:
```xml
<lang.foldingBuilder
    language="Elixir"
    implementationClass="dev.murek.elixirij.ide.folding.ExFoldingBuilder"/>
```

#### 2. Custom Folding Regions (Optional)

Support `# region` / `# endregion` comments for manual folding:

```kotlin
class ExCustomFoldingBuilder : CustomFoldingBuilder()
```

### Documentation References

- [Folding Builder](https://plugins.jetbrains.com/docs/intellij/folding-builder.html) - Official IntelliJ Platform SDK docs
- [FoldingBuilder API](https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/lang/folding/FoldingBuilder.java)
- [FoldingBuilderEx API](https://github.com/JetBrains/intellij-community/blob/master/platform/core-api/src/com/intellij/lang/folding/FoldingBuilderEx.java)
- [Code example: SimpleFoldingBuilder](https://github.com/JetBrains/intellij-sdk-code-samples/blob/main/simple_language_plugin/src/main/java/org/intellij/sdk/language/SimpleFoldingBuilder.java)

### Implementation Notes

#### Token-Based Approach
Since the parser is incomplete, use a **token-based approach** initially:

1. Scan PSI tree for elements with matching delimiters
2. Use `ExTypes` to identify structure types:
   - `EX_LBRACKET` / `EX_RBRACKET` for lists
   - `EX_PERCENT_LBRACE` / `EX_RBRACE` for maps
   - `EX_LBRACE` / `EX_RBRACE` for tuples
   - `EX_LT_LT` / `EX_GT_GT` for bitstrings
   - `EX_HEREDOC`, `EX_CHARLIST_HEREDOC` for heredocs

3. Create `FoldingDescriptor` for each matching pair

#### Example Implementation

```kotlin
override fun buildFoldRegions(root: PsiElement, document: Document, quick: Boolean): Array<FoldingDescriptor> {
    val descriptors = mutableListOf<FoldingDescriptor>()

    root.accept(object : PsiRecursiveElementVisitor() {
        override fun visitElement(element: PsiElement) {
            when (element) {
                is ExList -> addFoldingDescriptor(element, "[...]", descriptors)
                is ExMap -> addFoldingDescriptor(element, "%{...}", descriptors)
                is ExTuple -> addFoldingDescriptor(element, "{...}", descriptors)
                is ExBitstring -> addFoldingDescriptor(element, "<<...>>", descriptors)
                is ExHeredoc, is ExCharlistHeredoc ->
                    addFoldingDescriptor(element, "\"\"\"...\"\"\"", descriptors)
            }
            super.visitElement(element)
        }
    })

    return descriptors.toTypedArray()
}

private fun addFoldingDescriptor(
    element: PsiElement,
    placeholder: String,
    descriptors: MutableList<FoldingDescriptor>
) {
    if (element.textRange.length > 2) { // Only fold non-empty structures
        descriptors.add(FoldingDescriptor(
            element.node,
            element.textRange,
            null,
            placeholder
        ))
    }
}
```

#### Folding Options

Add settings in `ExCodeFoldingOptionsProvider`:

```kotlin
class ExCodeFoldingOptionsProvider : CodeFoldingOptionsProvider {
    override fun getOptions(): Array<BeanConfigurable<CodeFoldingSettings>> {
        return arrayOf(
            BeanConfigurable(CodeFoldingSettings("ELIXIR_HEREDOCS", "Heredocs")),
            BeanConfigurable(CodeFoldingSettings("ELIXIR_LARGE_COLLECTIONS", "Large collections"))
        )
    }
}
```

### Testing

Create `ExFoldingTest` extending `CodeInsightTestFixture`:

```kotlin
class ExFoldingTest : BasePlatformTestCase() {
    fun `test fold list`() {
        myFixture.configureByText("test.ex", """
            [
              1,
              2,
              3
            ]
        """.trimIndent())

        val foldingModel = myFixture.editor.foldingModel
        val regions = foldingModel.allFoldRegions

        assertEquals(1, regions.size)
        assertEquals("[...]", regions[0].placeholderText)
    }
}
```

Test files in: `src/test/testData/folding/`

### Acceptance Criteria

- [ ] Lists, maps, tuples, bitstrings fold correctly
- [ ] Heredocs and multi-line sigils fold
- [ ] Placeholder text is appropriate for structure type
- [ ] Nested structures fold independently
- [ ] Settings page allows customization
- [ ] Tests cover all foldable structures
- [ ] Works with incomplete/invalid code (error recovery)

### Related Issues

- Parser completion will enable folding for do-blocks, modules, functions

---

## Issue 2: Implement brace matching and auto-pairing for Elixir delimiters

### Description

Add intelligent brace matching and auto-pairing support for all Elixir delimiter types to improve editing experience and reduce syntax errors.

### Features to Implement

#### Brace Matching
Highlight matching pairs when cursor is near:
- Parentheses: `()`
- Brackets: `[]`
- Braces: `{}`
- Angle brackets (bitstrings): `<<>>`
- Map literals: `%{}`
- String delimiters: `"..."`, `'...'`
- Atom quotes: `:"..."`
- Sigil delimiters: `~r/.../`, `~s(...)`, etc.
- Heredoc delimiters: `"""..."""`, `'''...'''`

#### Auto-Pairing
Automatically insert closing delimiter:
- Type `(` → inserts `()` with cursor between
- Type `[` → inserts `[]` with cursor between
- Type `{` → inserts `{}` with cursor between
- Type `<<` → inserts `<<>>` with cursor between
- Type `%{` → inserts `%{}` with cursor between
- Smart quote pairing for strings/atoms
- Context-aware (don't auto-pair inside strings/comments)

#### Smart Behaviors
- **Overtype closing delimiter**: Typing `)` when next char is `)` moves cursor forward
- **Delete pair**: Backspace on `(` deletes matching `)` if nothing between
- **Wrap selection**: Select text and type `(` wraps selection in `(...)`
- **Context awareness**: Don't auto-pair inside strings, comments, or when preceded by escape

### IntelliJ Platform Extension Points

#### 1. `com.intellij.lang.braceMatcher`

**Implementation**: Create `ExBraceMatcher` implementing `BraceMatcher` or `PairedBraceMatcher`

```kotlin
class ExBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = arrayOf(
        BracePair(EX_LPAREN, EX_RPAREN, false),
        BracePair(EX_LBRACKET, EX_RBRACKET, false),
        BracePair(EX_LBRACE, EX_RBRACE, false),
        BracePair(EX_LT_LT, EX_GT_GT, false),
        BracePair(EX_PERCENT_LBRACE, EX_RBRACE, false),
        // Add more pairs as needed
    )

    override fun isPairedBracesAllowedBeforeType(
        lbraceType: IElementType,
        contextType: IElementType?
    ): Boolean {
        // Allow auto-pairing before whitespace, closing braces, etc.
        return contextType == null ||
               contextType == TokenType.WHITE_SPACE ||
               contextType in CLOSING_BRACES
    }

    override fun getCodeConstructStart(
        file: PsiFile,
        openingBraceOffset: Int
    ): Int = openingBraceOffset
}
```

**Register in** `plugin.xml`:
```xml
<lang.braceMatcher
    language="Elixir"
    implementationClass="dev.murek.elixirij.ide.ExBraceMatcher"/>
```

#### 2. `com.intellij.lang.quoteHandler`

**Implementation**: Create `ExQuoteHandler` for smart quote pairing

```kotlin
class ExQuoteHandler : SimpleTokenSetQuoteHandler(
    EX_STRING,
    EX_CHARLIST,
    EX_ATOM_QUOTED
) {
    override fun isOpeningQuote(
        iterator: HighlighterIterator,
        offset: Int
    ): Boolean {
        // Check if this is opening quote based on context
    }

    override fun isClosingQuote(
        iterator: HighlighterIterator,
        offset: Int
    ): Boolean {
        // Check if this is closing quote based on context
    }
}
```

**Register in** `plugin.xml`:
```xml
<lang.quoteHandler
    language="Elixir"
    implementationClass="dev.murek.elixirij.ide.ExQuoteHandler"/>
```

#### 3. `com.intellij.typedHandler` (Optional Advanced)

For complex auto-pairing logic:

```kotlin
class ExTypedHandler : TypedHandlerDelegate() {
    override fun charTyped(
        c: Char,
        project: Project,
        editor: Editor,
        file: PsiFile
    ): Result {
        if (file !is ExFile) return Result.CONTINUE

        when (c) {
            '<' -> handlePotentialBitstring(editor)
            '%' -> handlePotentialMap(editor)
        }

        return Result.CONTINUE
    }
}
```

### Documentation References

- [Brace Matcher](https://plugins.jetbrains.com/docs/intellij/brace-matching.html) - Official docs
- [BraceMatcher API](https://github.com/JetBrains/intellij-community/blob/master/platform/analysis-api/src/com/intellij/lang/BraceMatcher.java)
- [PairedBraceMatcher API](https://github.com/JetBrains/intellij-community/blob/master/platform/analysis-api/src/com/intellij/lang/PairedBraceMatcher.java)
- [QuoteHandler API](https://github.com/JetBrains/intellij-community/blob/master/platform/lang-api/src/com/intellij/codeInsight/editorActions/QuoteHandler.java)
- [Code example: SimpleBraceMatcher](https://github.com/JetBrains/intellij-sdk-code-samples/blob/main/simple_language_plugin/src/main/java/org/intellij/sdk/language/SimpleBraceMatcher.java)

### Implementation Notes

#### Basic Brace Pairs

```kotlin
class ExBraceMatcher : PairedBraceMatcher {
    override fun getPairs(): Array<BracePair> = arrayOf(
        BracePair(EX_LPAREN, EX_RPAREN, false),
        BracePair(EX_LBRACKET, EX_RBRACKET, false),
        BracePair(EX_LBRACE, EX_RBRACE, false),
        BracePair(EX_LT_LT, EX_GT_GT, false),
    )

    override fun isPairedBracesAllowedBeforeType(
        lbraceType: IElementType,
        contextType: IElementType?
    ): Boolean {
        // Don't auto-pair inside strings or comments
        if (contextType == EX_STRING ||
            contextType == EX_CHARLIST ||
            contextType == EX_COMMENT) {
            return false
        }

        // Allow before whitespace, closing braces, operators, etc.
        return contextType == null ||
               contextType == TokenType.WHITE_SPACE ||
               contextType in CLOSING_TOKENS ||
               contextType in OPERATOR_TOKENS
    }

    override fun getCodeConstructStart(file: PsiFile, openingBraceOffset: Int): Int {
        return openingBraceOffset
    }
}

private val CLOSING_TOKENS = TokenSet.create(
    EX_RPAREN, EX_RBRACKET, EX_RBRACE, EX_GT_GT
)

private val OPERATOR_TOKENS = TokenSet.create(
    EX_COMMA, EX_SEMICOLON, EX_PIPE, EX_FAT_ARROW, EX_COLON
)
```

#### Special Cases

##### Map Literals (`%{}`)
Map literals need special handling since `%{` is a compound token:

```kotlin
// In ExTypedHandler
override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
    if (c == '{' && isPrecededByPercent(editor)) {
        // Auto-insert closing }
        insertClosingBrace(editor)
    }
    return Result.CONTINUE
}
```

##### Bitstrings (`<<>>`)
Similar handling for `<<` sequence:

```kotlin
override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
    if (c == '<' && isPrecededByLessThan(editor)) {
        // Auto-insert closing >>
        insertText(editor, ">>")
        editor.caretModel.moveCaretRelatively(-2, 0, false, false, false)
    }
    return Result.CONTINUE
}
```

### Testing

```kotlin
class ExBraceMatcherTest : BasePlatformTestCase() {
    fun `test match parentheses`() {
        myFixture.configureByText("test.ex", "foo(<caret>)")
        val braceHighlighter = BraceHighlightingHandler.lookupBraceHighlighter(myFixture.editor, myFixture.file)
        // Assert brace matching works
    }

    fun `test auto-pair brackets`() {
        myFixture.configureByText("test.ex", "<caret>")
        myFixture.type('[')
        myFixture.checkResult("[<caret>]")
    }

    fun `test no auto-pair in string`() {
        myFixture.configureByText("test.ex", "\"<caret>\"")
        myFixture.type('(')
        myFixture.checkResult("\"(<caret>\"")
    }
}
```

### Acceptance Criteria

- [ ] All delimiter pairs are matched and highlighted
- [ ] Auto-pairing works for all delimiter types
- [ ] Smart behaviors (overtype, delete pair, wrap) work
- [ ] Context-aware (no auto-pair in strings/comments)
- [ ] Map literals `%{}` auto-pair correctly
- [ ] Bitstrings `<<>>` auto-pair correctly
- [ ] Quote handling works for strings, charlists, atoms
- [ ] Tests cover all delimiter types and edge cases
- [ ] Works with user settings (respects "auto-insert" preferences)

---

## Issue 3: Implement smart Enter handler for Elixir

### Description

Add intelligent Enter key handling to automatically indent code, continue multi-line constructs, and provide context-aware line breaks in Elixir code.

### Features to Implement

#### Auto-Indentation
- Indent after opening delimiters: `(`, `[`, `{`, `<<`, `%{`
- Maintain indentation level in lists, maps, tuples
- Un-indent on closing delimiters
- Future: Indent after `do` keyword (when parser supports it)

#### Smart Line Continuation
- Continue keyword lists across lines (align values)
- Continue binary operators (maintain indentation)
- Continue pipe chains (indent piped calls)
- Continue strings/charlists with proper quoting
- Continue multi-line function calls

#### Context-Aware Behaviors
- **Between delimiters**: `[<caret>]` →
  ```elixir
  [
    <caret>
  ]
  ```
- **In maps**: Format as:
  ```elixir
  %{
    key: value,
    <caret>
  }
  ```
- **In keyword lists**: Align colons:
  ```elixir
  [
    foo: 1,
    <caret>
  ]
  ```
- **After operators**: Maintain chain:
  ```elixir
  x
  |> foo()
  <caret>
  ```

#### Special Cases
- Split string literals with proper concatenation
- Handle heredocs (don't break)
- Comment continuation with `#` prefix
- Respect `.editorconfig` indentation settings

### IntelliJ Platform Extension Points

#### 1. `com.intellij.lang.smartEnterProcessor`

**Implementation**: Create `ExSmartEnterProcessor`

```kotlin
class ExSmartEnterProcessor : SmartEnterProcessor() {
    override fun process(
        project: Project,
        editor: Editor,
        psiFile: PsiFile
    ): Boolean {
        // Handle Ctrl+Shift+Enter (complete statement)
        // Auto-insert missing closing delimiters
        // Jump cursor to appropriate position
        return false
    }
}
```

**Register in** `plugin.xml`:
```xml
<lang.smartEnterProcessor
    language="Elixir"
    implementationClass="dev.murek.elixirij.ide.ExSmartEnterProcessor"/>
```

#### 2. `com.intellij.codeInsight.editorActions.enterHandler`

**Implementation**: Create `ExEnterHandler`

```kotlin
class ExEnterHandler : EnterHandlerDelegateAdapter() {
    override fun preprocessEnter(
        file: PsiFile,
        editor: Editor,
        caretOffset: Ref<Int>,
        caretAdvance: Ref<Int>,
        dataContext: DataContext,
        originalHandler: EditorActionHandler?
    ): EnterHandlerDelegate.Result {
        if (file !is ExFile) return Result.Continue

        val offset = caretOffset.get()
        val element = file.findElementAt(offset) ?: return Result.Continue

        // Handle special cases
        when {
            isBetweenBraces(element, offset) -> handleBraceSplit(editor, offset)
            isInKeywordList(element) -> handleKeywordContinuation(editor, offset)
            isAfterPipe(element) -> handlePipeContinuation(editor, offset)
            isInString(element) -> handleStringSplit(editor, offset)
        }

        return Result.Continue
    }
}
```

**Register in** `plugin.xml`:
```xml
<enterHandlerDelegate
    implementation="dev.murek.elixirij.ide.ExEnterHandler"
    order="first"/>
```

### Documentation References

- [Enter Handler](https://plugins.jetbrains.com/docs/intellij/typing-assistance.html#enter-handler) - Official docs
- [Smart Enter](https://plugins.jetbrains.com/docs/intellij/typing-assistance.html#smart-enter) - Complete statement feature
- [EnterHandlerDelegate API](https://github.com/JetBrains/intellij-community/blob/master/platform/lang-api/src/com/intellij/codeInsight/editorActions/enter/EnterHandlerDelegate.java)
- [SmartEnterProcessor API](https://github.com/JetBrains/intellij-community/blob/master/platform/lang-api/src/com/intellij/codeInsight/editorActions/smartEnter/SmartEnterProcessor.java)
- [Code example: Python EnterHandler](https://github.com/JetBrains/intellij-community/blob/master/python/src/com/jetbrains/python/editor/PyEnterHandler.java)

### Testing

```kotlin
class ExEnterHandlerTest : BasePlatformTestCase() {
    fun `test enter between brackets`() {
        myFixture.configureByText("test.ex", "[<caret>]")
        myFixture.type('\n')
        myFixture.checkResult("[\n  <caret>\n]")
    }

    fun `test enter in keyword list`() {
        myFixture.configureByText("test.ex", """
            [
              foo: 1,<caret>
            ]
        """.trimIndent())
        myFixture.type('\n')
        myFixture.checkResult("""
            [
              foo: 1,
              <caret>
            ]
        """.trimIndent())
    }
}
```

### Acceptance Criteria

- [ ] Auto-indent after opening delimiters
- [ ] Smart split between matching braces
- [ ] Keyword list continuation with proper alignment
- [ ] Pipe chain continuation with indentation
- [ ] String splitting with concatenation operator
- [ ] Smart Enter completes unclosed delimiters
- [ ] Respects `.editorconfig` indentation settings
- [ ] Works with nested structures
- [ ] Tests cover all scenarios

---

## Issue 4: Implement quote handler for intelligent quote pairing

### Description

Add intelligent quote handling for strings, charlists, and atoms in Elixir to automatically pair quotes, handle escape sequences, and provide context-aware quoting behavior.

### Features to Implement

#### Auto-Pairing Quotes
- **Strings**: Type `"` → inserts `""` with cursor between
- **Charlists**: Type `'` → inserts `''` with cursor between
- **Quoted atoms**: Type `:` then `"` → inserts `:""`
- **Heredocs**: Type `"""` → inserts `"""\n\n"""` with cursor between
- **Charlist heredocs**: Type `'''` → inserts `'''\n\n'''` with cursor between

#### Smart Behaviors
- **Overtype closing quote**: Typing `"` when next char is `"` moves cursor forward
- **Don't pair escaped quotes**: After `\`, typing `"` inserts literal `\"`
- **Context awareness**: Only pair quotes in code context, not in strings/comments
- **Selection wrapping**: Select text and type `"` wraps selection in `"..."`

#### Interpolation Support
- Inside strings, recognize `#{` for interpolation
- Don't pair quotes inside interpolation blocks
- Handle escaped interpolation `\#{`

### IntelliJ Platform Extension Points

#### 1. `com.intellij.lang.quoteHandler`

**Implementation**: Create `ExQuoteHandler`

```kotlin
class ExQuoteHandler : QuoteHandler {
    override fun isOpeningQuote(
        iterator: HighlighterIterator,
        offset: Int
    ): Boolean

    override fun isClosingQuote(
        iterator: HighlighterIterator,
        offset: Int
    ): Boolean

    override fun hasNonClosedLiteral(
        editor: Editor,
        iterator: HighlighterIterator,
        offset: Int
    ): Boolean

    override fun isInsideLiteral(iterator: HighlighterIterator): Boolean
}
```

**Register in** `plugin.xml`:
```xml
<lang.quoteHandler
    language="Elixir"
    implementationClass="dev.murek.elixirij.ide.ExQuoteHandler"/>
```

### Documentation References

- [Quote Handler](https://plugins.jetbrains.com/docs/intellij/typing-assistance.html#quote-handler) - Official docs
- [QuoteHandler API](https://github.com/JetBrains/intellij-community/blob/master/platform/lang-api/src/com/intellij/codeInsight/editorActions/QuoteHandler.java)
- [SimpleTokenSetQuoteHandler](https://github.com/JetBrains/intellij-community/blob/master/platform/lang-api/src/com/intellij/codeInsight/editorActions/SimpleTokenSetQuoteHandler.java)
- [Code example: JsonQuoteHandler](https://github.com/JetBrains/intellij-community/blob/master/json/src/com/intellij/json/editor/JsonQuoteHandler.java)

### Testing

```kotlin
class ExQuoteHandlerTest : BasePlatformTestCase() {
    fun `test auto-pair double quotes`() {
        myFixture.configureByText("test.ex", "<caret>")
        myFixture.type('"')
        myFixture.checkResult("\"<caret>\"")
    }

    fun `test overtype closing quote`() {
        myFixture.configureByText("test.ex", "\"hello<caret>\"")
        myFixture.type('"')
        myFixture.checkResult("\"hello\"<caret>")
    }

    fun `test heredoc insertion`() {
        myFixture.configureByText("test.ex", "\"\"<caret>")
        myFixture.type('"')
        myFixture.checkResult("\"\"\"\n  <caret>\n  \"\"\"")
    }
}
```

### Acceptance Criteria

- [ ] Auto-pair `"..."` for strings
- [ ] Auto-pair `'...'` for charlists
- [ ] Auto-pair `:"..."` for quoted atoms
- [ ] Heredoc auto-insertion with `"""`
- [ ] Overtype behavior for closing quotes
- [ ] Escape sequence handling
- [ ] Context awareness
- [ ] Interpolation awareness
- [ ] Selection wrapping
- [ ] Tests cover all quote types

---

## Issue 5: Configure spell checking for Elixir files

### Description

Configure IntelliJ Platform's spell checker to work intelligently with Elixir code, enabling spell checking in appropriate contexts (comments, strings, documentation) while excluding code elements.

### Features to Implement

#### Enable Spell Checking In:
- **Comments**: `# This is a comment`
- **String literals**: `"hello world"`
- **Charlist literals**: `'hello world'`
- **Heredocs**: `"""..."""` and `'''...'''`
- **Module documentation**: `@moduledoc "..."`
- **Function documentation**: `@doc "..."`
- **Sigil strings**: `~s(hello world)`

#### Disable Spell Checking In:
- **Code identifiers**: Variable names, function names
- **Atoms**: `:atom`, `:"quoted atom"`
- **Module aliases**: `MyModule.SubModule`
- **Keywords**: `def`, `defmodule`, `do`, `end`, etc.
- **Numbers and operators**
- **Regex sigils**: `~r/pattern/`
- **Code in interpolations**: `"#{variable_name}"`

#### Custom Dictionary:
- Elixir keywords and built-in modules
- Common Erlang terms (erlang, ets, dets, gen_server, etc.)
- Phoenix framework terms
- Ecto terms
- ExUnit terms

### IntelliJ Platform Extension Points

#### 1. `com.intellij.spellchecker.support`

**Implementation**: Create `ExSpellcheckingStrategy`

```kotlin
class ExSpellcheckingStrategy : SpellcheckingStrategy() {
    override fun getTokenizer(element: PsiElement): Tokenizer<*> {
        return when (element.node?.elementType) {
            EX_COMMENT -> TEXT_TOKENIZER
            EX_STRING, EX_CHARLIST, EX_HEREDOC -> StringLiteralTokenizer()
            else -> EMPTY_TOKENIZER
        }
    }

    override fun isMyContext(element: PsiElement): Boolean {
        return element.containingFile is ExFile
    }
}
```

**Register in** `plugin.xml`:
```xml
<spellchecker.support
    language="Elixir"
    implementationClass="dev.murek.elixirij.ide.spellcheck.ExSpellcheckingStrategy"/>
```

#### 2. Custom Dictionary

Create `elixir.dic` with common terms and register:

```xml
<spellchecker.dictionary.customDictionaryProvider
    implementation="dev.murek.elixirij.ide.spellcheck.ExCustomDictionaryProvider"/>
```

### Documentation References

- [Spell Checking](https://plugins.jetbrains.com/docs/intellij/spell-checking.html) - Official docs
- [SpellcheckingStrategy API](https://github.com/JetBrains/intellij-community/blob/master/spellchecker/src/com/intellij/spellchecker/tokenizer/SpellcheckingStrategy.java)
- [Custom Dictionary Guide](https://plugins.jetbrains.com/docs/intellij/spell-checking.html#custom-dictionaries)
- [Code example: Properties SpellcheckingStrategy](https://github.com/JetBrains/intellij-community/blob/master/plugins/properties/properties-psi-impl/src/com/intellij/lang/properties/spellchecker/PropertiesSpellcheckingStrategy.java)

### Testing

```kotlin
class ExSpellcheckingTest : BasePlatformTestCase() {
    fun `test spell check in comment`() {
        myFixture.configureByText("test.ex", "# This is a commnet")
        val typos = myFixture.doHighlighting()
            .filter { it.severity == HighlightSeverity.TYPO }
        assertEquals(1, typos.size)
    }

    fun `test no spell check in code`() {
        myFixture.configureByText("test.ex", "defmodule MyModul do\nend")
        val typos = myFixture.doHighlighting()
            .filter { it.severity == HighlightSeverity.TYPO }
        assertEquals(0, typos.size)
    }
}
```

### Acceptance Criteria

- [ ] Spell checking works in comments
- [ ] Spell checking works in string literals
- [ ] Spell checking works in heredocs
- [ ] Spell checking works in `@doc` and `@moduledoc`
- [ ] No spell checking in code identifiers
- [ ] Interpolations are skipped in strings
- [ ] Custom dictionary includes common Elixir/Erlang terms
- [ ] snake_case identifiers are not flagged
- [ ] Tests cover all scenarios
- [ ] User can add custom words to dictionary
