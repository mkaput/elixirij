package dev.murek.elixirij.lexer

import com.intellij.lexer.Lexer
import com.intellij.testFramework.LexerTestCase

/**
 * Tests for the Elixir lexer.
 */
class ExLexerTest : LexerTestCase() {

    override fun createLexer(): Lexer = ExLexer()
    override fun getDirPath(): String = ""

    fun testIntegerLiterals() = doTest(
        "42 0x1F 0o77 0b1010",
        """INTEGER ('42')
WHITE_SPACE (' ')
INTEGER ('0x1F')
WHITE_SPACE (' ')
INTEGER ('0o77')
WHITE_SPACE (' ')
INTEGER ('0b1010')
"""
    )

    fun testFloatLiterals() = doTest(
        "3.14 1.0e10 2.5e-3",
        """FLOAT ('3.14')
WHITE_SPACE (' ')
FLOAT ('1.0e10')
WHITE_SPACE (' ')
FLOAT ('2.5e-3')
"""
    )

    fun testKeywords() = doTest(
        "do end fn true false nil when in not and or",
        """do ('do')
WHITE_SPACE (' ')
end ('end')
WHITE_SPACE (' ')
fn ('fn')
WHITE_SPACE (' ')
true ('true')
WHITE_SPACE (' ')
false ('false')
WHITE_SPACE (' ')
nil ('nil')
WHITE_SPACE (' ')
when ('when')
WHITE_SPACE (' ')
in ('in')
WHITE_SPACE (' ')
not ('not')
WHITE_SPACE (' ')
and ('and')
WHITE_SPACE (' ')
or ('or')
"""
    )

    fun testIdentifiers() = doTest(
        "foo bar_baz hello123 _private",
        """IDENTIFIER ('foo')
WHITE_SPACE (' ')
IDENTIFIER ('bar_baz')
WHITE_SPACE (' ')
IDENTIFIER ('hello123')
WHITE_SPACE (' ')
IDENTIFIER ('_private')
"""
    )

    fun testAliases() = doTest(
        "Foo BarBaz MyModule",
        """ALIAS ('Foo')
WHITE_SPACE (' ')
ALIAS ('BarBaz')
WHITE_SPACE (' ')
ALIAS ('MyModule')
"""
    )

    fun testAtoms() = doTest(
        ":foo :bar_baz",
        """ATOM (':foo')
WHITE_SPACE (' ')
ATOM (':bar_baz')
"""
    )

    fun testQuotedAtoms() = doTest(
        ":\"quoted atom\"",
        """ATOM_QUOTED (':"quoted atom"')
"""
    )

    fun testStrings() = doTest(
        "\"hello\" \"world\\n\"",
        """STRING ('"hello"')
WHITE_SPACE (' ')
STRING ('"world\n"')
"""
    )

    fun testCharlists() = doTest(
        "'hello' 'world\\n'",
        """CHARLIST (''hello'')
WHITE_SPACE (' ')
CHARLIST (''world\n'')
"""
    )

    fun testComments() = doTest(
        "# this is a comment\nfoo",
        """COMMENT ('# this is a comment')
EOL ('\n')
IDENTIFIER ('foo')
"""
    )

    fun testOperators() = doTest(
        "+ - * / = == != < > <= >= && || |> -> =>",
        """+ ('+')
WHITE_SPACE (' ')
- ('-')
WHITE_SPACE (' ')
* ('*')
WHITE_SPACE (' ')
/ ('/')
WHITE_SPACE (' ')
= ('=')
WHITE_SPACE (' ')
== ('==')
WHITE_SPACE (' ')
!= ('!=')
WHITE_SPACE (' ')
< ('<')
WHITE_SPACE (' ')
> ('>')
WHITE_SPACE (' ')
<= ('<=')
WHITE_SPACE (' ')
>= ('>=')
WHITE_SPACE (' ')
&& ('&&')
WHITE_SPACE (' ')
|| ('||')
WHITE_SPACE (' ')
|> ('|>')
WHITE_SPACE (' ')
-> ('->')
WHITE_SPACE (' ')
=> ('=>')
"""
    )

    fun testDelimiters() = doTest(
        "()[]{}",
        """( ('(')
) (')')
[ ('[')
] (']')
{ ('{')
} ('}')
"""
    )

    fun testCharLiterals() = doTest(
        "?a ?\\n ?\\t",
        """CHAR ('?a')
WHITE_SPACE (' ')
CHAR ('?\n')
WHITE_SPACE (' ')
CHAR ('?\t')
"""
    )

    fun testRangeOperators() = doTest(
        "1..10 1...10",
        """INTEGER ('1')
.. ('..')
INTEGER ('10')
WHITE_SPACE (' ')
INTEGER ('1')
... ('...')
INTEGER ('10')
"""
    )

    fun testSimpleElixirFunction() = doTest(
        """def hello(name) do
  "Hello, " <> name
end""",
        """IDENTIFIER ('def')
WHITE_SPACE (' ')
IDENTIFIER ('hello')
( ('(')
IDENTIFIER ('name')
) (')')
WHITE_SPACE (' ')
do ('do')
EOL ('\n')
WHITE_SPACE ('  ')
STRING ('"Hello, "')
WHITE_SPACE (' ')
<> ('<>')
WHITE_SPACE (' ')
IDENTIFIER ('name')
EOL ('\n')
end ('end')
"""
    )
}
