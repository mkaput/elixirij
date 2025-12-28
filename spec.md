# ElixirIJ Parser Port Specification

This document describes the plan to port Expert Language Server's Elixir parser to ElixirIJ's Grammar-Kit-powered parser
and PSI system.

## Source Analysis

### Expert Parser Structure

Expert uses a **Yecc parser** (`future_elixir_parser.yrl`) and an **Erlang tokenizer** (`future_elixir_tokenizer.erl`).
These are copies of Elixir's core parser with modifications for IDE features like cursor completion.

Key characteristics:

- The parser produces an AST in Elixir's standard quoted form: `{atom, metadata, args}`
- Metadata includes line, column, and token information
- Uses operator precedence parsing with 20+ precedence levels
- Has special handling for "matched", "unmatched", and "no_parens" expressions
- Supports do-blocks, fn expressions, and all Elixir constructs

### Elixir Grammar Terminals (from parser.yrl)

```
Terminals:
  identifier kw_identifier kw_identifier_safe kw_identifier_unsafe bracket_identifier
  paren_identifier do_identifier block_identifier op_identifier
  fn 'end' alias
  atom atom_quoted atom_safe atom_unsafe bin_string list_string sigil
  bin_heredoc list_heredoc
  comp_op at_op unary_op and_op or_op arrow_op match_op in_op in_match_op ellipsis_op
  type_op dual_op mult_op power_op concat_op range_op xor_op pipe_op stab_op when_op
  capture_int capture_op assoc_op rel_op ternary_op dot_call_op
  'true' 'false' 'nil' 'do' eol ';' ',' '.'
  '(' ')' '[' ']' '{' '}' '<<' '>>' '%{}' '%'
  int flt char
```

### Operator Precedence (from parser.yrl)

| Precedence | Associativity | Operators                                  |
|------------|---------------|--------------------------------------------|
| 5          | Left          | do                                         |
| 10         | Right         | -> (stab)                                  |
| 20         | Left          | ,                                          |
| 40         | Left          | <-, \\\\ (in_match)                        |
| 50         | Right         | when                                       |
| 60         | Right         | :: (type)                                  |
| 70         | Right         | \| (pipe)                                  |
| 80         | Right         | => (assoc)                                 |
| 90         | Nonassoc      | &, ... (capture, ellipsis)                 |
| 100        | Right         | = (match)                                  |
| 120        | Left          | \|\|, \|\|\|, or                           |
| 130        | Left          | &&, &&&, and                               |
| 140        | Left          | ==, !=, =~, ===, !==                       |
| 150        | Left          | <, >, <=, >=                               |
| 160        | Left          | \|>, <<<, >>>, <<~, ~>>, <~, ~>, <~>, <\|> |
| 170        | Left          | in, not in                                 |
| 180        | Left          | ^^^                                        |
| 190        | Right         | // (ternary)                               |
| 200        | Right         | ++, --, +++, ---, <> (concat/range)        |
| 210        | Left          | +, - (dual)                                |
| 220        | Left          | *, / (mult)                                |
| 230        | Left          | ** (power)                                 |
| 300        | Nonassoc      | +, -, !, ^, not, ~~~ (unary)               |
| 310        | Left          | ., .() (dot)                               |
| 320        | Nonassoc      | @                                          |

## PSI Structure Design

The PSI (Program Structure Interface) hierarchy follows Elixir's AST structure closely:

### Root Elements

```
ExFile
└── topLevelExpressions
    └── expression*
```

### Expression Hierarchy

```
expression
├── matchedExpr      // expr op expr (standard)
├── unmatchedExpr    // with do-block (if/case/etc.)
├── noParensExpr     // function calls without parens
├── containerExpr    // inside lists/maps/etc.
└── blockExpr        // with do-block attached
```

### Primary Expressions (Leaves and Simple Constructs)

```
accessExpr
├── literal
│   ├── integer
│   ├── float
│   ├── char
│   ├── string
│   ├── charlist
│   ├── heredoc
│   ├── true
│   ├── false
│   └── nil
├── atom
│   ├── atomSimple        // :atom
│   └── atomQuoted        // :"atom with spaces"
├── identifier            // variable or function name
├── alias                 // Module.Name
├── list                  // [1, 2, 3]
├── tuple                 // {1, 2, 3}
├── map                   // %{key: value}
├── struct                // %Struct{key: value}
├── bitstring             // <<1, 2, 3>>
├── sigil                 // ~r/regex/
├── fn                    // fn -> end
├── parenExpr             // (expr)
├── bracketAccess         // expr[key]
├── captureExpr           // &function/arity
└── unaryExpr             // @, !, ^, not, etc.
```

### Binary Operations

```
binaryOp
├── matchOp               // =
├── typeOp                // ::
├── pipeOp                // |
├── assocOp               // =>
├── whenOp                // when
├── stabOp                // ->
├── inOp                  // in
├── inMatchOp             // <-, \\
├── orOp                  // ||, |||, or
├── andOp                 // &&, &&&, and
├── compOp                // ==, !=, =~, ===, !==
├── relOp                 // <, >, <=, >=
├── arrowOp               // |>, <<<, >>>, etc.
├── xorOp                 // ^^^
├── ternaryOp             // //
├── concatOp              // ++, --, +++, ---, <>
├── rangeOp               // ..
├── dualOp                // +, -
├── multOp                // *, /
├── powerOp               // **
└── dotOp                 // .
```

### Call Expressions

```
call
├── parenCall             // func(args)
├── noParensCall          // func arg1, arg2
├── dotCall               // Mod.func(args)
├── anonymousCall         // func.()
├── qualifiedCall         // Module.function
├── bracketCall           // func[key]
└── captureCall           // &func/arity
```

### Blocks and Control Flow

```
doBlock
├── do                    // do ... end
├── after                 // after ...
├── else                  // else ...
├── catch                 // catch ...
└── rescue                // rescue ...

stabClause                // pattern -> body
```

### Keyword Lists and Arguments

```
keywordPair               // key: value
keywordList               // [key: value, ...]
arguments                 // (arg1, arg2, key: value)
```

## Grammar-Kit BNF Structure

The Grammar-Kit `.bnf` file will define the grammar using the following structure:

```bnf
{
  // Configuration as in current Elixir.bnf
}

// Root
root ::= topLevelExpressions

topLevelExpressions ::= (eoe? expression (eoe expression)*)? eoe?

// Expressions - following operator precedence
expression ::= matchOp | unmatchedExpr | noParensExpr

// Binary operators by precedence (right to left)
matchOp ::= orOp (EX_EQ orOp)*
orOp ::= andOp ((EX_PIPE_PIPE | EX_PIPE_PIPE_PIPE | EX_OR) andOp)*
andOp ::= compOp ((EX_AMP_AMP | EX_AMP_AMP_AMP | EX_AND) compOp)*
compOp ::= relOp ((EX_EQ_EQ | EX_NOT_EQ | ...) relOp)*
relOp ::= arrowOp ((EX_LT | EX_GT | ...) arrowOp)*
// ... continue for all precedence levels

// Unary and primary
unaryExpr ::= unaryOp unaryExpr | accessExpr
accessExpr ::= primaryExpr bracketAccess*

primaryExpr ::= literal
              | atom
              | identifier
              | alias
              | list
              | tuple
              | map
              | struct
              | bitstring
              | sigil
              | fnExpr
              | parenExpr
              | captureExpr

// Literals
literal ::= integer | float | char | string | charlist | heredoc
          | EX_TRUE | EX_FALSE | EX_NIL

integer ::= EX_INTEGER
float ::= EX_FLOAT
// ... etc.

// Data structures
list ::= EX_LBRACKET listContents? EX_RBRACKET
tuple ::= EX_LBRACE tupleContents? EX_RBRACE
map ::= EX_PERCENT_LBRACE mapContents? EX_RBRACE
struct ::= EX_PERCENT alias EX_LBRACE mapContents? EX_RBRACE
bitstring ::= EX_LT_LT bitstringContents? EX_GT_GT

// Calls
parenCall ::= callTarget EX_LPAREN arguments? EX_RPAREN doBlock?
noParensCall ::= callTarget noParensArgs doBlock?
dotCall ::= accessExpr EX_DOT callTarget arguments?

// Do blocks
doBlock ::= EX_DO doContents EX_END
doContents ::= stabClause* | expression* blockClause*
blockClause ::= (EX_ELSE | EX_AFTER | EX_CATCH | EX_RESCUE) stabClause*
stabClause ::= stabArgs? EX_ARROW expression

// Anonymous functions
fnExpr ::= EX_FN stabClause+ EX_END

// Keyword pairs
keywordPair ::= keywordKey EX_COLON expression
keywordKey ::= EX_IDENTIFIER | EX_ATOM | string
```

## Test Cases

The following test cases will be ported from Expert and adapted for IntelliJ's fixture-based testing:

### 1. Literals

| Test Name                 | Input             | Description               |
|---------------------------|-------------------|---------------------------|
| `testInteger`             | `42`              | Simple integer            |
| `testIntegerHex`          | `0xFF`            | Hexadecimal integer       |
| `testIntegerOctal`        | `0o777`           | Octal integer             |
| `testIntegerBinary`       | `0b1010`          | Binary integer            |
| `testIntegerUnderscore`   | `1_000_000`       | Integer with underscores  |
| `testFloat`               | `3.14`            | Simple float              |
| `testFloatScientific`     | `1.0e10`          | Scientific notation       |
| `testChar`                | `?a`              | Character literal         |
| `testCharEscaped`         | `?\n`             | Escaped character         |
| `testString`              | `"hello"`         | Simple string             |
| `testStringInterpolation` | `"hello #{name}"` | String with interpolation |
| `testStringEscape`        | `"hello\nworld"`  | String with escapes       |
| `testHeredoc`             | `"""..."""`       | Heredoc string            |
| `testCharlist`            | `'hello'`         | Charlist                  |
| `testCharlistHeredoc`     | `'''...'''`       | Heredoc charlist          |

### 2. Atoms

| Test Name                     | Input                 | Description                    |
|-------------------------------|-----------------------|--------------------------------|
| `testAtomSimple`              | `:atom`               | Simple atom                    |
| `testAtomQuoted`              | `:"atom with spaces"` | Quoted atom                    |
| `testAtomQuotedInterpolation` | `:"atom_#{var}"`      | Quoted atom with interpolation |
| `testAtomOperator`            | `:+`                  | Operator atom                  |
| `testAtomNil`                 | `:nil`                | Nil as atom                    |
| `testAtomBoolean`             | `:true`               | Boolean as atom                |

### 3. Identifiers and Aliases

| Test Name                  | Input              | Description           |
|----------------------------|--------------------|-----------------------|
| `testIdentifierSimple`     | `foo`              | Simple identifier     |
| `testIdentifierWithSuffix` | `foo?`             | Identifier with ?     |
| `testIdentifierWithBang`   | `foo!`             | Identifier with !     |
| `testIdentifierUnderscore` | `_unused`          | Underscore identifier |
| `testAliasSimple`          | `MyModule`         | Simple alias          |
| `testAliasNested`          | `My.Nested.Module` | Nested alias          |
| `testAliasSpecial`         | `__MODULE__`       | Special alias         |

### 4. Operators

| Test Name            | Input            | Description           |
|----------------------|------------------|-----------------------|
| `testOpMatch`        | `x = 1`          | Match operator        |
| `testOpPipe`         | `x \|> f`        | Pipe operator         |
| `testOpCompare`      | `x == y`         | Comparison            |
| `testOpArithmetic`   | `1 + 2 * 3`      | Arithmetic precedence |
| `testOpLogical`      | `a and b or c`   | Logical operators     |
| `testOpIn`           | `x in [1, 2, 3]` | In operator           |
| `testOpRange`        | `1..10`          | Range                 |
| `testOpRangeStep`    | `1..10//2`       | Range with step       |
| `testOpConcat`       | `[1] ++ [2]`     | List concatenation    |
| `testOpStringConcat` | `"a" <> "b"`     | String concatenation  |
| `testOpType`         | `x :: integer`   | Type spec             |
| `testOpCapture`      | `&func/1`        | Capture operator      |
| `testOpPin`          | `^x = y`         | Pin operator          |

### 5. Data Structures

| Test Name             | Input                    | Description        |
|-----------------------|--------------------------|--------------------|
| `testListEmpty`       | `[]`                     | Empty list         |
| `testListSimple`      | `[1, 2, 3]`              | Simple list        |
| `testListKeyword`     | `[a: 1, b: 2]`           | Keyword list       |
| `testListMixed`       | `[1, a: 2]`              | Mixed list         |
| `testListCons`        | `[h \| t]`               | Cons pattern       |
| `testTupleEmpty`      | `{}`                     | Empty tuple        |
| `testTupleSimple`     | `{1, 2}`                 | Simple tuple       |
| `testMapEmpty`        | `%{}`                    | Empty map          |
| `testMapSimple`       | `%{a: 1}`                | Simple map         |
| `testMapArrow`        | `%{"key" => 1}`          | Map with arrow     |
| `testMapUpdate`       | `%{map \| a: 1}`         | Map update         |
| `testStructSimple`    | `%User{}`                | Simple struct      |
| `testStructFields`    | `%User{name: "foo"}`     | Struct with fields |
| `testStructUpdate`    | `%{user \| name: "bar"}` | Struct update      |
| `testBitstringEmpty`  | `<<>>`                   | Empty bitstring    |
| `testBitstringSimple` | `<<1, 2, 3>>`            | Simple bitstring   |
| `testBitstringTyped`  | `<<x::binary>>`          | Typed bitstring    |

### 6. Sigils

| Test Name            | Input          | Description          |
|----------------------|----------------|----------------------|
| `testSigilRegex`     | `~r/pattern/`  | Regex sigil          |
| `testSigilString`    | `~s(string)`   | String sigil         |
| `testSigilWordlist`  | `~w(a b c)`    | Wordlist sigil       |
| `testSigilCharlist`  | `~c'charlist'` | Charlist sigil       |
| `testSigilHeredoc`   | `~S"""..."""`  | Heredoc sigil        |
| `testSigilModifiers` | `~r/pattern/i` | Sigil with modifiers |
| `testSigilCustom`    | `~H"""..."""`  | Custom sigil         |

### 7. Function Calls

| Test Name                 | Input                       | Description           |
|---------------------------|-----------------------------|-----------------------|
| `testCallParen`           | `foo(1, 2)`                 | Parenthesized call    |
| `testCallNoParen`         | `foo 1, 2`                  | No-paren call         |
| `testCallNoArgs`          | `foo()`                     | No arguments          |
| `testCallKeyword`         | `foo(a: 1)`                 | Keyword argument      |
| `testCallMixed`           | `foo(1, a: 2)`              | Mixed arguments       |
| `testCallPipeline`        | `x \|> foo() \|> bar()`     | Pipeline              |
| `testCallNested`          | `foo(bar(1))`               | Nested calls          |
| `testCallAnonymous`       | `func.()`                   | Anonymous call        |
| `testCallQualified`       | `Mod.func()`                | Qualified call        |
| `testCallQualifiedNested` | `A.B.func()`                | Nested qualified call |
| `testCallRemote`          | `:erlang.term_to_binary(x)` | Remote call           |
| `testCallBracket`         | `list[0]`                   | Bracket access        |
| `testCallChained`         | `a.b.c`                     | Chained access        |

### 8. Anonymous Functions

| Test Name              | Input                      | Description        |
|------------------------|----------------------------|--------------------|
| `testFnSimple`         | `fn -> :ok end`            | Simple fn          |
| `testFnOneArg`         | `fn x -> x end`            | One argument       |
| `testFnMultiArg`       | `fn x, y -> x + y end`     | Multiple arguments |
| `testFnMultiClause`    | `fn :a -> 1; :b -> 2 end`  | Multiple clauses   |
| `testFnPattern`        | `fn {a, b} -> a end`       | Pattern matching   |
| `testFnGuard`          | `fn x when x > 0 -> x end` | With guard         |
| `testCaptureNamed`     | `&func/1`                  | Named capture      |
| `testCaptureAnonymous` | `&(&1 + &2)`               | Anonymous capture  |
| `testCaptureQualified` | `&Mod.func/2`              | Qualified capture  |

### 9. Do Blocks and Control Flow

| Test Name              | Input                                                  | Description          |
|------------------------|--------------------------------------------------------|----------------------|
| `testDoBlockSimple`    | `do :ok end`                                           | Simple do-end        |
| `testDoBlockMultiExpr` | `do a; b end`                                          | Multiple expressions |
| `testIfSimple`         | `if true do :ok end`                                   | Simple if            |
| `testIfElse`           | `if true do :ok else :err end`                         | If-else              |
| `testIfOneLiner`       | `if(true, do: :ok, else: :err)`                        | One-liner if         |
| `testUnless`           | `unless false do :ok end`                              | Unless               |
| `testCase`             | `case x do :a -> 1 end`                                | Case expression      |
| `testCaseMulti`        | `case x do :a -> 1; :b -> 2 end`                       | Multi-clause case    |
| `testCaseGuard`        | `case x do x when x > 0 -> x end`                      | Case with guard      |
| `testCond`             | `cond do true -> :ok end`                              | Cond expression      |
| `testWith`             | `with {:ok, x} <- f() do x end`                        | With expression      |
| `testWithElse`         | `with {:ok, x} <- f() do x else _ -> :err end`         | With-else            |
| `testTry`              | `try do f() rescue _ -> :err end`                      | Try-rescue           |
| `testTryCatch`         | `try do f() catch :throw, x -> x end`                  | Try-catch            |
| `testTryAfter`         | `try do f() after cleanup() end`                       | Try-after            |
| `testReceive`          | `receive do {:msg, x} -> x end`                        | Receive              |
| `testReceiveAfter`     | `receive do {:msg, x} -> x after 1000 -> :timeout end` | Receive-after        |

### 10. Module Definition

| Test Name             | Input                                      | Description         |
|-----------------------|--------------------------------------------|---------------------|
| `testDefmodule`       | `defmodule Foo do end`                     | Empty module        |
| `testDefmoduleNested` | `defmodule A.B do end`                     | Nested module       |
| `testDef`             | `def foo, do: :ok`                         | Function definition |
| `testDefMultiClause`  | `def foo(:a), do: 1; def foo(:b), do: 2`   | Multi-clause        |
| `testDefp`            | `defp foo, do: :ok`                        | Private function    |
| `testDefmacro`        | `defmacro foo, do: :ok`                    | Macro               |
| `testDefmacrop`       | `defmacrop foo, do: :ok`                   | Private macro       |
| `testDefguard`        | `defguard is_pos(x) when x > 0`            | Guard               |
| `testDefstruct`       | `defstruct [:a, :b]`                       | Struct definition   |
| `testDefexception`    | `defexception [:message]`                  | Exception           |
| `testDefprotocol`     | `defprotocol P do def f(x) end`            | Protocol            |
| `testDefimpl`         | `defimpl P, for: T do def f(x), do: x end` | Implementation      |

### 11. Module Attributes

| Test Name        | Input                    | Description      |
|------------------|--------------------------|------------------|
| `testModuleAttr` | `@attr value`            | Simple attribute |
| `testModuleDoc`  | `@moduledoc "..."`       | Module doc       |
| `testDoc`        | `@doc "..."`             | Function doc     |
| `testSpec`       | `@spec foo(t) :: t`      | Type spec        |
| `testType`       | `@type t :: atom`        | Type definition  |
| `testOpaque`     | `@opaque t :: atom`      | Opaque type      |
| `testCallback`   | `@callback foo(t) :: t`  | Callback         |
| `testBehaviour`  | `@behaviour MyBehaviour` | Behaviour        |

### 12. Import/Alias/Require/Use

| Test Name          | Input                          | Description        |
|--------------------|--------------------------------|--------------------|
| `testAlias`        | `alias Foo.Bar`                | Simple alias       |
| `testAliasAs`      | `alias Foo.Bar, as: B`         | Alias with as      |
| `testAliasMulti`   | `alias Foo.{A, B}`             | Multi-alias        |
| `testImport`       | `import Foo`                   | Simple import      |
| `testImportOnly`   | `import Foo, only: [bar: 1]`   | Import with only   |
| `testImportExcept` | `import Foo, except: [bar: 1]` | Import with except |
| `testRequire`      | `require Foo`                  | Require            |
| `testUse`          | `use Foo`                      | Use                |
| `testUseOpts`      | `use Foo, opt: :val`           | Use with options   |

### 13. Pattern Matching

| Test Name             | Input                       | Description      |
|-----------------------|-----------------------------|------------------|
| `testPatternVariable` | `x = 1`                     | Variable pattern |
| `testPatternIgnore`   | `_ = expr`                  | Ignore pattern   |
| `testPatternPin`      | `^x = y`                    | Pin pattern      |
| `testPatternTuple`    | `{a, b} = tuple`            | Tuple pattern    |
| `testPatternList`     | `[h \| t] = list`           | List pattern     |
| `testPatternMap`      | `%{key: v} = map`           | Map pattern      |
| `testPatternStruct`   | `%User{name: n} = user`     | Struct pattern   |
| `testPatternBinary`   | `<<h, rest::binary>> = bin` | Binary pattern   |
| `testPatternNested`   | `%{a: {b, [c \| _]}} = x`   | Nested pattern   |

### 14. Comprehensions

| Test Name          | Input                                  | Description         |
|--------------------|----------------------------------------|---------------------|
| `testForSimple`    | `for x <- list, do: x`                 | Simple for          |
| `testForFilter`    | `for x <- list, x > 0, do: x`          | For with filter     |
| `testForMultiGen`  | `for x <- a, y <- b, do: {x, y}`       | Multiple generators |
| `testForInto`      | `for x <- list, into: %{}, do: {x, x}` | For with into       |
| `testForUniq`      | `for x <- list, uniq: true, do: x`     | For with uniq       |
| `testForBitstring` | `for <<x <- bin>>, do: x`              | Bitstring generator |

### 15. Special Forms

| Test Name            | Input                                | Description          |
|----------------------|--------------------------------------|----------------------|
| `testQuote`          | `quote do: x`                        | Quote                |
| `testQuoteUnquote`   | `quote do: unquote(x)`               | Quote with unquote   |
| `testQuoteSplice`    | `quote do: unquote_splicing(list)`   | Quote with splice    |
| `testQuoteBind`      | `quote bind_quoted: [x: x] do x end` | Quote with binding   |
| `testSuper`          | `super()`                            | Super call           |
| `testRaise`          | `raise "error"`                      | Raise                |
| `testRaiseException` | `raise MyError, message: "err"`      | Raise with exception |
| `testThrow`          | `throw :value`                       | Throw                |
| `testSend`           | `send pid, msg`                      | Send message         |

### 16. Edge Cases and Valid Syntax

| Test Name                | Input                       | Description            |
|--------------------------|-----------------------------|------------------------|
| `testEmptyFile`          | ``                          | Empty file             |
| `testOnlyWhitespace`     | `   \n  \n  `               | Only whitespace        |
| `testOnlyComments`       | `# comment`                 | Only comments          |
| `testTrailingComma`      | `[1, 2, 3,]`                | Trailing comma         |
| `testNewlinesInExpr`     | `1 +\n2`                    | Newlines in expression |
| `testMultipleStatements` | `a; b; c`                   | Semicolon-separated    |
| `testNestedDoBlocks`     | `if a do if b do c end end` | Nested blocks          |
| `testAmbiguousCall`      | `foo bar baz`               | Ambiguous call         |
| `testOperatorAsAtom`     | `:+`                        | Operator as atom       |

### 17. Partial Parsing and Error Recovery

These tests verify the parser handles incomplete/invalid code gracefully, which is critical for IDE functionality.
The parser should produce a partial PSI tree with error nodes rather than failing completely.

#### 17.1 Unclosed Delimiters

| Test Name                      | Input                          | Description                        |
|--------------------------------|--------------------------------|------------------------------------|
| `testPartialListUnclosed`      | `[1, 2, 3`                     | Unclosed list                      |
| `testPartialTupleUnclosed`     | `{1, 2`                        | Unclosed tuple                     |
| `testPartialMapUnclosed`       | `%{a: 1`                       | Unclosed map                       |
| `testPartialBitstringUnclosed` | `<<1, 2`                       | Unclosed bitstring                 |
| `testPartialParenUnclosed`     | `(1 + 2`                       | Unclosed parenthesis               |
| `testPartialStringUnclosed`    | `"hello`                       | Unclosed string                    |
| `testPartialHeredocUnclosed`   | `"""\nhello`                   | Unclosed heredoc                   |
| `testPartialSigilUnclosed`     | `~r/pattern`                   | Unclosed sigil                     |
| `testPartialNestedUnclosed`    | `[{1, 2}, {3`                  | Nested unclosed delimiters         |
| `testPartialDeeplyNested`      | `[[[[1`                        | Deeply nested unclosed             |

#### 17.2 Incomplete Do-Blocks

| Test Name                       | Input                                  | Description                   |
|---------------------------------|----------------------------------------|-------------------------------|
| `testPartialDoNoEnd`            | `if true do\n  :ok`                    | Missing end                   |
| `testPartialDoEmpty`            | `if true do`                           | Empty do block, missing end   |
| `testPartialDoNestedNoEnd`      | `if a do\n  if b do\n    :ok`          | Nested missing end            |
| `testPartialDoOnlyEnd`          | `end`                                  | Orphan end keyword            |
| `testPartialFnNoEnd`            | `fn x -> x`                            | Missing end for fn            |
| `testPartialFnNoArrow`          | `fn x`                                 | Missing arrow and body        |
| `testPartialCaseNoEnd`          | `case x do\n  :a -> 1`                 | Case missing end              |
| `testPartialCaseNoArrow`        | `case x do\n  :a`                      | Case clause missing arrow     |
| `testPartialWithNoEnd`          | `with {:ok, x} <- f() do\n  x`         | With missing end              |
| `testPartialTryNoEnd`           | `try do\n  f()`                        | Try missing end               |
| `testPartialReceiveNoEnd`       | `receive do\n  {:msg, x} -> x`         | Receive missing end           |
| `testPartialCondNoEnd`          | `cond do\n  true -> :ok`               | Cond missing end              |

#### 17.3 Incomplete Module Definitions

| Test Name                          | Input                               | Description                    |
|------------------------------------|-------------------------------------|--------------------------------|
| `testPartialDefmoduleNoEnd`        | `defmodule Foo do`                  | Module missing end             |
| `testPartialDefmoduleNoBody`       | `defmodule Foo`                     | Module missing do-block        |
| `testPartialDefNoBody`             | `def foo`                           | Function missing body          |
| `testPartialDefNoEnd`              | `def foo do\n  :ok`                 | Function missing end           |
| `testPartialDefIncompleteArgs`     | `def foo(a,`                        | Incomplete argument list       |
| `testPartialDefIncompleteGuard`    | `def foo(x) when`                   | Incomplete guard               |
| `testPartialDefmacroNoEnd`         | `defmacro foo do`                   | Macro missing end              |
| `testPartialDefstructIncomplete`   | `defstruct [`                       | Incomplete defstruct           |
| `testPartialDefprotocolNoEnd`      | `defprotocol P do`                  | Protocol missing end           |
| `testPartialDefimplNoEnd`          | `defimpl P, for: T do`              | Implementation missing end     |

#### 17.4 Incomplete Expressions

| Test Name                         | Input                    | Description                      |
|-----------------------------------|--------------------------|----------------------------------|
| `testPartialBinaryOpNoRhs`        | `1 +`                    | Binary op missing RHS            |
| `testPartialBinaryOpNoLhs`        | `+ 1`                    | Unary interpretation (valid)     |
| `testPartialPipelineIncomplete`   | `x \|>`                  | Pipeline missing RHS             |
| `testPartialMatchNoRhs`           | `x =`                    | Match missing RHS                |
| `testPartialComparisonNoRhs`      | `x ==`                   | Comparison missing RHS           |
| `testPartialRangeNoEnd`           | `1..`                    | Range missing end                |
| `testPartialTypeSpecNoRhs`        | `x ::`                   | Type spec missing type           |
| `testPartialCaptureIncomplete`    | `&Mod.func/`             | Capture missing arity            |
| `testPartialCaptureNoSlash`       | `&Mod.func`              | Capture missing /arity           |
| `testPartialArrowNoRhs`           | `x ->`                   | Stab arrow missing body          |
| `testPartialWhenNoGuard`          | `x when`                 | When missing guard expression    |

#### 17.5 Incomplete Function Calls

| Test Name                          | Input                        | Description                     |
|------------------------------------|------------------------------|---------------------------------|
| `testPartialCallUnclosedParen`     | `foo(1, 2`                   | Call with unclosed paren        |
| `testPartialCallTrailingComma`     | `foo(1, 2,`                  | Call with trailing comma        |
| `testPartialCallNoArgs`            | `foo(`                       | Call with empty unclosed paren  |
| `testPartialCallKeywordNoValue`    | `foo(a:`                     | Keyword arg missing value       |
| `testPartialCallKeywordIncomplete` | `foo(a: 1,`                  | Incomplete keyword list         |
| `testPartialDotCallIncomplete`     | `Mod.`                       | Dot without function name       |
| `testPartialDotCallChained`        | `a.b.`                       | Chained dot incomplete          |
| `testPartialAnonymousCall`         | `func.(`                     | Anonymous call unclosed         |
| `testPartialBracketAccess`         | `list[`                      | Bracket access unclosed         |
| `testPartialNestedCallIncomplete`  | `foo(bar(`                   | Nested calls unclosed           |

#### 17.6 Incomplete Data Structures

| Test Name                          | Input                        | Description                     |
|------------------------------------|------------------------------|---------------------------------|
| `testPartialListTrailingComma`     | `[1, 2,`                     | List trailing comma             |
| `testPartialListConsIncomplete`    | `[h \|`                      | Cons pattern incomplete         |
| `testPartialMapKeyNoValue`         | `%{a:`                       | Map key without value           |
| `testPartialMapArrowNoValue`       | `%{"key" =>`                 | Map arrow without value         |
| `testPartialMapUpdateIncomplete`   | `%{map \|`                   | Map update incomplete           |
| `testPartialStructNoFields`        | `%User{`                     | Struct unclosed                 |
| `testPartialStructIncomplete`      | `%User{name:`                | Struct field incomplete         |
| `testPartialKeywordNoValue`        | `[a: 1, b:`                  | Keyword list incomplete         |
| `testPartialTupleTrailingComma`    | `{1, 2,`                     | Tuple trailing comma            |
| `testPartialBitstringTyped`        | `<<x::`                      | Bitstring type incomplete       |

#### 17.7 Incomplete Special Forms

| Test Name                          | Input                               | Description                   |
|------------------------------------|-------------------------------------|-------------------------------|
| `testPartialAliasIncomplete`       | `alias Foo.`                        | Alias with trailing dot       |
| `testPartialAliasAsIncomplete`     | `alias Foo, as:`                    | Alias as: incomplete          |
| `testPartialAliasMultiIncomplete`  | `alias Foo.{A,`                     | Multi-alias unclosed          |
| `testPartialImportOnlyIncomplete`  | `import Foo, only: [`               | Import only: unclosed         |
| `testPartialQuoteNoBody`           | `quote do:`                         | Quote missing body            |
| `testPartialUnquoteIncomplete`     | `quote do: unquote(`                | Unquote unclosed              |
| `testPartialForNoBody`             | `for x <- list`                     | For missing do                |
| `testPartialForGeneratorIncompl`   | `for x <-`                          | For generator incomplete      |
| `testPartialWithClauseIncomplete`  | `with {:ok, x} <-`                  | With clause incomplete        |

#### 17.8 Mixed Incomplete Scenarios

| Test Name                            | Input                                        | Description                         |
|--------------------------------------|----------------------------------------------|-------------------------------------|
| `testPartialModuleWithIncomplete`    | `defmodule Foo do\n  def bar(`               | Module with incomplete function     |
| `testPartialNestedBlocksIncomplete`  | `if a do\n  case b do\n    :c ->`            | Nested incomplete blocks            |
| `testPartialPipelineInBlock`         | `def foo do\n  x \|>`                        | Incomplete pipeline in function     |
| `testPartialCallInList`              | `[foo(1,`                                    | Incomplete call inside list         |
| `testPartialMapInCall`               | `foo(%{a:`                                   | Incomplete map in call              |
| `testPartialMultipleErrors`          | `[1, {2, def foo do`                         | Multiple incomplete constructs      |
| `testPartialValidThenInvalid`        | `x = 1\ny =`                                 | Valid expr followed by incomplete   |
| `testPartialInvalidThenValid`        | `x =\ny = 1`                                 | Incomplete followed by valid        |
| `testPartialAtModuleLevel`           | `defmodule A do\nend\ndefmodule B do`        | Second module incomplete            |
| `testPartialInterpolationUnclosed`   | `"hello #{`                                  | Unclosed interpolation              |

#### 17.9 Error Recovery Verification

| Test Name                            | Input                                        | Expected Behavior                   |
|--------------------------------------|----------------------------------------------|-------------------------------------|
| `testRecoveryAfterBadExpr`           | `[\n  1,\n  +,\n  2\n]`                      | Parse list despite bad element      |
| `testRecoverySkipBadStatement`       | `x = 1\n@#$%\ny = 2`                         | Parse valid statements around error |
| `testRecoveryInModuleBody`           | `defmodule F do\n  @#$\n  def f, do: 1\nend` | Parse module despite bad attr       |
| `testRecoveryNestedStructures`       | `%{a: [1, {2,], b: 3}`                       | Recover in nested structures        |
| `testRecoveryContinueAfterEnd`       | `if true do\nend end\nx = 1`                 | Continue after extra end            |
| `testRecoveryMismatchedDelimiters`   | `[1, 2}`                                     | Handle mismatched delimiters        |

## Implementation Plan

**Note:** Completed phases are marked with ✅ DONE in their heading.

### Phase 1: Core Grammar (Basic Expressions) ✅ DONE

1. Extend `Elixir.bnf` with complete expression grammar
2. Handle operator precedence correctly
3. Implement all literal types
4. Add data structure parsing (list, tuple, map, struct, bitstring)

---

### Phase 2: Missing Operators ✅ DONE

Add operators not yet implemented in the expression hierarchy.

**Grammar additions:**
- Type operator `::` (precedence 60, right associative)
- Pipe operator `|` for patterns (precedence 70, right associative)
- When operator `when` (precedence 50, right associative)
- Stab operator `->` (precedence 10, right associative)
- In-match operators `<-`, `\\` (precedence 40, left associative)

**Tests:** `testOpType`, `testOpWhen`, `testOpStab`, `testOpInMatch`

---

### Phase 3: Parenthesized Function Calls (Positional Arguments) ✅ DONE

Implement function calls with parentheses, supporting positional arguments only.

**Grammar additions:**
- `parenCall ::= EX_LPAREN callArgs? EX_RPAREN`
- `dotAccess ::= EX_DOT (identifier | alias)`
- `accessExpr ::= primaryExpr (dotAccess | bracketAccess | parenCall)*`
- `callArgs ::= containerExpr (EX_COMMA containerExpr)* EX_COMMA?`

**Tests:** `testCallParen`, `testCallNoArgs`, `testCallNested`, `testCallQualified`, `testCallQualifiedNested`, `testCallRemote`

**Note:** Keyword arguments (`testCallKeyword`, `testCallMixed`) are deferred to Phase 3.5.

---

### Phase 3.5: Keyword Arguments in Function Calls ✅ DONE

Implement keyword argument support in function calls using lexer-level tokens.

**Problem:** Grammar-Kit's PEG parser cannot distinguish `a` (identifier expression) from `a:` (keyword key start) without lookahead at the parser level. Both `keywordPair | containerExpr` alternatives match `EX_IDENTIFIER`, causing the parser to commit to the wrong path.

**Solution:** Follow Expert's approach with lexer-level whitespace-sensitive tokens:
- Emit `kw_identifier` token (instead of regular `identifier`) when tokenizer sees `identifier: <space>`
- This makes keyword pairs unambiguous at the token level
- Elixir language rule: keyword syntax requires space after colon (`a: 1` is valid, `a:1` is not)

**Lexer additions (Elixir.flex):**
- Add state management for tracking context after identifiers
- Emit `EX_KW_IDENTIFIER` token when pattern `IDENTIFIER ':' SPACE` is matched
- Regular `EX_IDENTIFIER` for all other cases

**Grammar updates:**
- `keywordKey ::= EX_KW_IDENTIFIER | EX_ATOM_QUOTED EX_COLON`
- Update `callArgs` to support mixed positional and keyword arguments

**Tests:** `testCallKeyword`, `testCallMixed`

---

### Phase 4: Anonymous Calls ✅ DONE

Implement anonymous function invocation.

**Grammar additions:**
- `anonymousCall ::= accessExpr EX_DOT EX_LPAREN arguments? EX_RPAREN`
- Update `accessExpr` to handle chained `.()` calls

**Tests:** `testCallAnonymous`

---

### Phase 5: Anonymous Functions (fn) ✅ DONE

Implement `fn -> end` expressions.

**Grammar additions:**
- `fnExpr ::= EX_FN stabClause+ EX_END`
- `stabClause ::= stabArgs? EX_ARROW stabBody`
- `stabArgs ::= stabArg (EX_COMMA stabArg)* whenClause?`
- `stabArg ::= expression`
- `stabBody ::= expression (eoe expression)*`
- `whenClause ::= EX_WHEN expression`

**Tests:** `testFnSimple`, `testFnOneArg`, `testFnMultiArg`, `testFnMultiClause`, `testFnPattern`, `testFnGuard`

---

### Phase 6: Capture Expressions ✅ DONE

Enhance capture operator for named and anonymous captures.

**Grammar additions:**
- `captureNamed ::= EX_AMPERSAND callTarget EX_SLASH EX_INTEGER`
- `captureAnonymous ::= EX_AMPERSAND parenExpr` (using &1, &2 placeholders)
- Capture placeholder: `EX_AMPERSAND EX_INTEGER`

**Tests:** `testCaptureNamed`, `testCaptureAnonymous`, `testCaptureQualified`, `testOpCapture` (enhance)

---

### Phase 7: Do-Blocks (Basic)

Implement basic do-end blocks.

**Grammar additions:**
- `doBlock ::= EX_DO doContents EX_END`
- `doContents ::= (eoe? expression (eoe expression)*)? eoe?`
- Inline variant: `doInline ::= EX_COMMA? EX_DO_COLON expression`

**Tests:** `testDoBlockSimple`, `testDoBlockMultiExpr`

---

### Phase 8: Do-Block Clauses

Add else/after/catch/rescue clauses to do-blocks.

**Grammar additions:**
- `blockClause ::= elseClause | afterClause | catchClause | rescueClause`
- `elseClause ::= EX_ELSE doContents`
- `afterClause ::= EX_AFTER doContents`
- `catchClause ::= EX_CATCH stabClause+`
- `rescueClause ::= EX_RESCUE stabClause+`

**Tests:** `testIfElse` (partial), `testTryCatch`, `testTryAfter`, `testTryRescue`

---

### Phase 9: No-Parens Function Calls

Implement function calls without parentheses.

**Grammar additions:**
- `noParensCall ::= callTarget noParensArgs`
- `noParensArgs ::= expression (EX_COMMA expression)* keywordTail?`
- `keywordTail ::= EX_COMMA keywordPair (EX_COMMA keywordPair)*`
- Handle ambiguous parsing with lookahead

**Tests:** `testCallNoParen`, `testAmbiguousCall`

---

### Phase 10: Calls with Do-Blocks

Integrate do-blocks with function calls.

**Grammar additions:**
- Update `parenCall` and `noParensCall` to accept optional `doBlock`
- Handle `func(args) do ... end` and `func args do ... end` patterns

**Tests:** `testIfSimple`, `testIfOneLiner`, `testUnless`

---

### Phase 11: Control Flow - case

Implement case expressions.

**Grammar additions:**
- `caseExpr ::= EX_CASE expression EX_DO caseClause+ EX_END`
- `caseClause ::= stabClause` (pattern -> body)
- Guards in patterns: `pattern when guard -> body`

**Tests:** `testCase`, `testCaseMulti`, `testCaseGuard`

---

### Phase 12: Control Flow - cond

Implement cond expressions.

**Grammar additions:**
- `condExpr ::= EX_COND EX_DO condClause+ EX_END`
- `condClause ::= expression EX_ARROW expression eoe?`

**Tests:** `testCond`

---

### Phase 13: Control Flow - with

Implement with expressions.

**Grammar additions:**
- `withExpr ::= EX_WITH withClause (EX_COMMA withClause)* doBlock`
- `withClause ::= expression EX_LEFT_ARROW expression`
- Support `else` clause for with

**Tests:** `testWith`, `testWithElse`

---

### Phase 14: Control Flow - try/rescue/catch/after

Implement try expressions.

**Grammar additions:**
- `tryExpr ::= EX_TRY EX_DO tryContents EX_END`
- `tryContents ::= doContents rescueClause* catchClause* afterClause?`
- Rescue patterns: `rescue e in Exception -> ...`

**Tests:** `testTry`, `testTryCatch`, `testTryAfter`

---

### Phase 15: Control Flow - receive

Implement receive expressions.

**Grammar additions:**
- `receiveExpr ::= EX_RECEIVE EX_DO receiveClause+ afterClause? EX_END`
- `receiveClause ::= stabClause`

**Tests:** `testReceive`, `testReceiveAfter`

---

### Phase 16: Module Definitions (defmodule)

Implement module definitions.

**Grammar additions:**
- `defmoduleExpr ::= EX_DEFMODULE alias doBlock`
- Module body contains definitions and expressions

**Tests:** `testDefmodule`, `testDefmoduleNested`

---

### Phase 17: Function Definitions (def/defp)

Implement function definitions.

**Grammar additions:**
- `defExpr ::= (EX_DEF | EX_DEFP) defHead defBody`
- `defHead ::= identifier defArgs? whenClause?`
- `defArgs ::= EX_LPAREN argList? EX_RPAREN`
- `defBody ::= doBlock | doInline`

**Tests:** `testDef`, `testDefMultiClause`, `testDefp`

---

### Phase 18: Macro Definitions (defmacro/defmacrop)

Implement macro definitions.

**Grammar additions:**
- `defmacroExpr ::= (EX_DEFMACRO | EX_DEFMACROP) defHead defBody`
- Reuse `defHead` and `defBody` from Phase 17

**Tests:** `testDefmacro`, `testDefmacrop`

---

### Phase 19: Guard Definitions (defguard)

Implement guard definitions.

**Grammar additions:**
- `defguardExpr ::= (EX_DEFGUARD | EX_DEFGUARDP) identifier defArgs? whenClause`

**Tests:** `testDefguard`

---

### Phase 20: Struct and Exception Definitions

Implement defstruct and defexception.

**Grammar additions:**
- `defstructExpr ::= EX_DEFSTRUCT (list | keywordList)`
- `defexceptionExpr ::= EX_DEFEXCEPTION (list | keywordList)`

**Tests:** `testDefstruct`, `testDefexception`

---

### Phase 21: Protocol Definitions

Implement defprotocol and defimpl.

**Grammar additions:**
- `defprotocolExpr ::= EX_DEFPROTOCOL alias doBlock`
- `defimplExpr ::= EX_DEFIMPL alias EX_COMMA EX_FOR_COLON alias doBlock`

**Tests:** `testDefprotocol`, `testDefimpl`

---

### Phase 22: Module Attributes

Implement module attributes.

**Grammar additions:**
- `moduleAttr ::= EX_AT identifier attrValue?`
- `attrValue ::= expression`
- Special handling for `@spec`, `@type`, `@doc`, etc.

**Tests:** `testModuleAttr`, `testModuleDoc`, `testDoc`, `testSpec`, `testType`, `testOpaque`, `testCallback`, `testBehaviour`

---

### Phase 23: Import/Require/Use

Implement import, require, and use.

**Grammar additions:**
- `importExpr ::= EX_IMPORT alias importOpts?`
- `importOpts ::= EX_COMMA keywordList`
- `requireExpr ::= EX_REQUIRE alias`
- `useExpr ::= EX_USE alias useOpts?`

**Tests:** `testImport`, `testImportOnly`, `testImportExcept`, `testRequire`, `testUse`, `testUseOpts`

---

### Phase 24: Alias Directive

Implement alias directive (not to be confused with alias literals).

**Grammar additions:**
- `aliasDirective ::= EX_ALIAS alias aliasOpts?`
- `aliasOpts ::= EX_COMMA EX_AS_COLON alias`
- Multi-alias: `EX_ALIAS alias EX_DOT EX_LBRACE alias (EX_COMMA alias)* EX_RBRACE`

**Tests:** `testAlias`, `testAliasAs`, `testAliasMulti`

---

### Phase 25: Comprehensions (for)

Implement for comprehensions.

**Grammar additions:**
- `forExpr ::= EX_FOR forClause (EX_COMMA forClause)* forOpts? doBlock`
- `forClause ::= forGenerator | forFilter`
- `forGenerator ::= expression EX_LEFT_ARROW expression`
- `forFilter ::= expression`
- `forOpts ::= EX_COMMA (EX_INTO_COLON | EX_UNIQ_COLON | EX_REDUCE_COLON) expression`

**Tests:** `testForSimple`, `testForFilter`, `testForMultiGen`, `testForInto`, `testForUniq`, `testForBitstring`

---

### Phase 26: Quote and Unquote

Implement quote special form.

**Grammar additions:**
- `quoteExpr ::= EX_QUOTE quoteOpts? doBlock`
- `quoteOpts ::= EX_COMMA keywordList`
- `unquoteExpr ::= EX_UNQUOTE EX_LPAREN expression EX_RPAREN`
- `unquoteSplicingExpr ::= EX_UNQUOTE_SPLICING EX_LPAREN expression EX_RPAREN`

**Tests:** `testQuote`, `testQuoteUnquote`, `testQuoteSplice`, `testQuoteBind`

---

### Phase 27: Raise, Throw, and Send

Implement remaining special forms.

**Grammar additions:**
- `raiseExpr ::= EX_RAISE expression (EX_COMMA expression)?`
- `throwExpr ::= EX_THROW expression`
- `sendExpr ::= EX_SEND expression EX_COMMA expression`
- `superExpr ::= EX_SUPER arguments?`

**Tests:** `testRaise`, `testRaiseException`, `testThrow`, `testSend`, `testSuper`

---

### Phase 28: Edge Cases and Validation

Handle remaining edge cases.

**Grammar refinements:**
- Trailing commas in all containers
- Newlines within expressions
- Multiple semicolon-separated statements
- Nested do-blocks
- Operator atoms (`:+`, `:-`, etc.)

**Tests:** All tests from section 16 (Edge Cases and Valid Syntax)

---

### Phase 29: Testing and Refinement

Port remaining test cases and validate complete grammar.

**Tasks:**
1. Ensure all tests from sections 1-16 pass
2. Add additional edge case tests as discovered
3. Verify PSI structure matches expected output
4. Profile parser performance on large files

---

### Phase 30: Partial Parsing and Error Recovery

This phase is critical for IDE functionality. Users constantly work with incomplete code, and the parser must handle this gracefully.

#### 30.1 Grammar-Kit Error Recovery Mechanisms

1. **Pin markers**: Use `pin` to mark tokens that commit to a rule, enabling better error messages and recovery
   ```bnf
   list ::= '[' list_contents? ']' { pin=1 }  // Pin on '[' - once we see it, we're committed to parsing a list
   ```

2. **Recovery rules**: Use `recoverWhile` to skip tokens until a recovery point
   ```bnf
   statement ::= expression { recoverWhile=statement_recovery }
   private statement_recovery ::= !(EOL | ';' | 'end' | 'do')
   ```

3. **Extending rules**: Use `extends` for better error node hierarchy

#### 30.2 Implementation Tasks

1. Add `pin` markers to all delimiter-based rules:
   - Lists: pin on `[`
   - Tuples: pin on `{`
   - Maps: pin on `%{`
   - Bitstrings: pin on `<<`
   - Parentheses: pin on `(`
   - Do-blocks: pin on `do`
   - Fn expressions: pin on `fn`

2. Add `recoverWhile` rules for:
   - Top-level expressions (recover at newlines, semicolons)
   - Container elements (recover at commas, closing delimiters)
   - Do-block contents (recover at block keywords: else, catch, rescue, after, end)
   - Function arguments (recover at commas, closing paren)
   - Module body (recover at def/defp/defmacro/end)

3. Implement smart recovery for common patterns:
   - Missing `end` keywords
   - Unclosed delimiters
   - Incomplete binary operations
   - Malformed function calls

4. Add PSI error element wrapping:
   - Create `ExErrorElement` for invalid/incomplete nodes
   - Ensure error elements contain recoverable content
   - Preserve as much valid PSI structure as possible

#### 30.3 Testing Strategy

1. **Unit tests**: Each partial parsing test case from section 17
2. **Property-based tests**: Generate random incomplete code fragments
3. **Real-world tests**: Use actual incomplete code from Expert's test fixtures
4. **IDE integration tests**: Verify completion/navigation works with incomplete code

#### 30.4 Success Criteria

- Parser never crashes or hangs on any input
- Valid portions of code produce correct PSI structure
- Error nodes are localized (don't consume valid following code)
- IDE features (completion, highlighting) work in files with errors
- Error recovery is consistent and predictable

## File Structure

```
src/
├── main/
│   ├── grammars/
│   │   └── Elixir.bnf           # Complete grammar definition
│   └── kotlin/
│       └── dev/murek/elixirij/lang/
│           ├── psi/
│           │   ├── ExElementType.kt    # Element types
│           │   ├── ExTokenType.kt      # Token types
│           │   └── impl/               # Generated PSI implementations
│           └── ElementTypes.kt         # Token definitions
└── test/
    ├── kotlin/
    │   └── dev/murek/elixirij/lang/
    │       └── ExParserTest.kt         # Parser test class
    └── testData/
        └── parser/
            ├── literals/
            ├── atoms/
            ├── operators/
            ├── dataStructures/
            ├── calls/
            ├── functions/
            ├── controlFlow/
            ├── modules/
            ├── edgeCases/
            └── partial/             # Phase 6: Partial parsing tests
                ├── unclosedDelimiters/
                ├── incompleteDoBlocks/
                ├── incompleteModules/
                ├── incompleteExpressions/
                ├── incompleteCalls/
                ├── incompleteDataStructures/
                ├── incompleteSpecialForms/
                ├── mixedIncomplete/
                └── errorRecovery/
```

## Notes

1. **Precedence Handling**: Grammar-Kit uses standard PEG/BNF precedence. We'll encode Elixir's operator precedence by
   structuring rules appropriately.

2. **Whitespace Sensitivity**: Elixir has some whitespace-sensitive constructs (e.g., space before `(` changes meaning).
   These need special handling in the parser.

3. **Newline Handling**: Elixir uses newlines as expression separators. The lexer already handles EOL tokens, but the
   parser needs to treat them correctly.

4. **Error Recovery**: Grammar-Kit supports `pin` and `recoverWhile` for error recovery. We'll use these to provide good
   IDE behavior for incomplete code.

5. **Performance**: Large files with many expressions may need optimization. We'll monitor parser performance during
   testing.

6. **Partial Parsing Philosophy**: IDE parsers must be tolerant of incomplete/invalid input. Key principles:
   - **Fail gracefully**: Never crash, always produce some PSI tree
   - **Localize errors**: Error nodes should be as small as possible
   - **Preserve context**: Valid surrounding code should parse correctly
   - **Enable features**: Completion, navigation, highlighting should work even with errors
   - **Consistent recovery**: Same incomplete input should produce same PSI structure

7. **Expert's Approach**: Expert uses `cursor_completion` mode in the tokenizer that prunes tokens and adds cursor
   markers for IDE features. Our approach differs - we use Grammar-Kit's native error recovery which produces error
   nodes in the PSI tree while still parsing surrounding valid code.

8. **Testing Partial Parsing**: Each partial parsing test should verify:
   - Parser doesn't crash
   - Error nodes are present and localized
   - Valid portions have correct PSI structure
   - PSI tree is navigable (parent/child relationships work)
