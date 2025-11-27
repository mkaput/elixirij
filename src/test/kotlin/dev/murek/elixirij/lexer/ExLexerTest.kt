package dev.murek.elixirij.lexer

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Tests for the Elixir lexer.
 */
class ExLexerTest {

    private fun tokenize(text: String): List<Pair<String, String>> {
        val lexer = ExLexer()
        lexer.start(text)
        val tokens = mutableListOf<Pair<String, String>>()
        while (lexer.tokenType != null) {
            val tokenText = text.substring(lexer.tokenStart, lexer.tokenEnd)
            tokens.add(lexer.tokenType.toString() to tokenText)
            lexer.advance()
        }
        return tokens
    }

    @Test
    fun `test integer literals`() {
        val tokens = tokenize("42 0x1F 0o77 0b1010")
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" }
        assertEquals(4, nonWs.size)
        assertEquals("INTEGER" to "42", nonWs[0])
        assertEquals("INTEGER" to "0x1F", nonWs[1])
        assertEquals("INTEGER" to "0o77", nonWs[2])
        assertEquals("INTEGER" to "0b1010", nonWs[3])
    }

    @Test
    fun `test float literals`() {
        val tokens = tokenize("3.14 1.0e10 2.5e-3")
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" }
        assertEquals(3, nonWs.size)
        assertEquals("FLOAT" to "3.14", nonWs[0])
        assertEquals("FLOAT" to "1.0e10", nonWs[1])
        assertEquals("FLOAT" to "2.5e-3", nonWs[2])
    }

    @Test
    fun `test keywords`() {
        val tokens = tokenize("do end fn true false nil when in not and or")
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" }
        assertEquals(11, nonWs.size)
        assertEquals("do" to "do", nonWs[0])
        assertEquals("end" to "end", nonWs[1])
        assertEquals("fn" to "fn", nonWs[2])
        assertEquals("true" to "true", nonWs[3])
        assertEquals("false" to "false", nonWs[4])
        assertEquals("nil" to "nil", nonWs[5])
        assertEquals("when" to "when", nonWs[6])
        assertEquals("in" to "in", nonWs[7])
        assertEquals("not" to "not", nonWs[8])
        assertEquals("and" to "and", nonWs[9])
        assertEquals("or" to "or", nonWs[10])
    }

    @Test
    fun `test identifiers`() {
        val tokens = tokenize("foo bar_baz hello123 _private")
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" }
        assertEquals(4, nonWs.size)
        nonWs.forEach { assertEquals("IDENTIFIER", it.first) }
    }

    @Test
    fun `test aliases`() {
        val tokens = tokenize("Foo BarBaz MyModule")
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" }
        assertEquals(3, nonWs.size)
        nonWs.forEach { assertEquals("ALIAS", it.first) }
    }

    @Test
    fun `test atoms`() {
        val tokens = tokenize(":foo :bar_baz :\"quoted atom\"")
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" }
        assertEquals(3, nonWs.size)
        assertEquals("ATOM" to ":foo", nonWs[0])
        assertEquals("ATOM" to ":bar_baz", nonWs[1])
        assertEquals("ATOM_QUOTED" to ":\"quoted atom\"", nonWs[2])
    }

    @Test
    fun `test strings`() {
        val tokens = tokenize("\"hello\" \"world\\n\"")
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" }
        assertEquals(2, nonWs.size)
        assertEquals("STRING" to "\"hello\"", nonWs[0])
        assertEquals("STRING" to "\"world\\n\"", nonWs[1])
    }

    @Test
    fun `test charlists`() {
        val tokens = tokenize("'hello' 'world\\n'")
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" }
        assertEquals(2, nonWs.size)
        assertEquals("CHARLIST" to "'hello'", nonWs[0])
        assertEquals("CHARLIST" to "'world\\n'", nonWs[1])
    }

    @Test
    fun `test comments`() {
        val tokens = tokenize("# this is a comment\nfoo")
        assertEquals("COMMENT" to "# this is a comment", tokens[0])
        assertEquals("EOL" to "\n", tokens[1])
        assertEquals("IDENTIFIER" to "foo", tokens[2])
    }

    @Test
    fun `test operators`() {
        val tokens = tokenize("+ - * / = == != < > <= >= && || |> -> =>")
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" }
        assertEquals("+", nonWs[0].second)
        assertEquals("-", nonWs[1].second)
        assertEquals("*", nonWs[2].second)
        assertEquals("/", nonWs[3].second)
        assertEquals("=", nonWs[4].second)
        assertEquals("==", nonWs[5].second)
        assertEquals("!=", nonWs[6].second)
        assertEquals("<", nonWs[7].second)
        assertEquals(">", nonWs[8].second)
        assertEquals("<=", nonWs[9].second)
        assertEquals(">=", nonWs[10].second)
        assertEquals("&&", nonWs[11].second)
        assertEquals("||", nonWs[12].second)
        assertEquals("|>", nonWs[13].second)
        assertEquals("->", nonWs[14].second)
        assertEquals("=>", nonWs[15].second)
    }

    @Test
    fun `test delimiters`() {
        val tokens = tokenize("()[]{}")
        assertEquals("(" to "(", tokens[0])
        assertEquals(")" to ")", tokens[1])
        assertEquals("[" to "[", tokens[2])
        assertEquals("]" to "]", tokens[3])
        assertEquals("{" to "{", tokens[4])
        assertEquals("}" to "}", tokens[5])
    }

    @Test
    fun `test char literals`() {
        val tokens = tokenize("?a ?\\n ?\\t")
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" }
        assertEquals(3, nonWs.size)
        nonWs.forEach { assertEquals("CHAR", it.first) }
    }

    @Test
    fun `test range operators`() {
        val tokens = tokenize("1..10 1...10")
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" }
        assertEquals(6, nonWs.size)
        assertEquals("INTEGER", nonWs[0].first)
        assertEquals("..", nonWs[1].second)
        assertEquals("INTEGER", nonWs[2].first)
        assertEquals("INTEGER", nonWs[3].first)
        assertEquals("...", nonWs[4].second)
        assertEquals("INTEGER", nonWs[5].first)
    }

    @Test
    fun `test simple elixir function`() {
        val code = """
            def hello(name) do
              "Hello, " <> name
            end
        """.trimIndent()
        val tokens = tokenize(code)
        val nonWs = tokens.filter { it.first != "WHITE_SPACE" && it.first != "EOL" }
        
        // Just verify it tokenizes without errors and produces expected tokens
        assert(nonWs.any { it.second == "def" })
        assert(nonWs.any { it.second == "hello" })
        assert(nonWs.any { it.second == "do" })
        assert(nonWs.any { it.second == "end" })
    }
}
