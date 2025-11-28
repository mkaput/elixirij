package dev.murek.elixirij.lexer

import com.intellij.lexer.Lexer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.testFramework.LexerTestCase
import kotlin.random.Random

private const val ATTEMPT_COUNT = 10_000
private const val STRING_LENGTH = 1024

/**
 * Fuzzing test for the Elixir lexer.
 *
 * This test generates random strings and verifies that the lexer can process them
 * without failing. The lexer should be able to handle any input, even if it produces
 * BAD_CHARACTER tokens for invalid content.
 *
 * The random seed is configurable via environment variable "ELIXIRIJ_FUZZ_SEED"
 * or system property "elixirij.fuzz.seed", allowing reproducible test runs.
 * If not specified, a random seed is used for each run.
 */
class ExLexerFuzzTest : LexerTestCase() {

    override fun createLexer(): Lexer = ExLexer()

    override fun getDirPath(): String = throw UnsupportedOperationException()

    fun `test fuzz lexer`() {
        val seed = getSeed()
        println("Running fuzz test with seed: $seed")
        val random = Random(seed)

        for (attempt in 1..ATTEMPT_COUNT) {
            val input = generateRandomString(random)
            try {
                val totalConsumed = tokenizeAndCountConsumed(input)
                assertEquals("Lexer did not consume entire input", input.length, totalConsumed)
            } catch (e: Exception) {
                val input = StringUtil.escapeStringCharacters(input)
                fail("Lexer threw exception on attempt $attempt (seed=$seed): ${e.message}\nInput:\n$input")
            }
        }
    }

    /**
     * Get the seed for the random number generator.
     * This can be configured via:
     * - Environment variable "ELIXIRIJ_FUZZ_SEED" (takes precedence)
     * - System property "elixirij.fuzz.seed"
     *
     * If neither is set, a random seed is generated.
     */
    private fun getSeed(): Long {
        val seedEnv = System.getenv("ELIXIRIJ_FUZZ_SEED")
        if (seedEnv != null) {
            return seedEnv.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid seed value in environment variable 'ELIXIRIJ_FUZZ_SEED': $seedEnv")
        }
        val seedProperty = System.getProperty("elixirij.fuzz.seed")
        if (seedProperty != null) {
            return seedProperty.toLongOrNull()
                ?: throw IllegalArgumentException("Invalid seed value in system property 'elixirij.fuzz.seed': $seedProperty")
        }
        return Random.nextLong()
    }

    /**
     * Generate a random string of the specified length using printable ASCII characters
     * and some common Unicode characters that might appear in Elixir code.
     */
    private fun generateRandomString(random: Random): String {
        val chars = CharArray(STRING_LENGTH)
        for (i in 0 until STRING_LENGTH) {
            chars[i] = generateRandomChar(random)
        }
        return String(chars)
    }

    /**
     * Generate a random character. Produces a mix of:
     * - Printable ASCII characters (space to tilde)
     * - Whitespace characters (newlines, carriage returns, tabs, spaces)
     * - Some Unicode characters
     */
    private fun generateRandomChar(random: Random): Char {
        return when (random.nextInt(100)) {
            in 0..79 -> {
                // 80% printable ASCII (space to tilde: 32-126)
                (32 + random.nextInt(95)).toChar()
            }

            in 80..89 -> {
                // 10% whitespace characters
                when (random.nextInt(4)) {
                    0 -> '\n'
                    1 -> '\r'
                    2 -> '\t'
                    else -> ' '
                }
            }

            else -> {
                // 10% various unicode characters
                when (random.nextInt(5)) {
                    0 -> (0x00C0 + random.nextInt(64)).toChar() // Latin Extended-A
                    1 -> (0x0400 + random.nextInt(256)).toChar() // Cyrillic
                    2 -> (0x4E00 + random.nextInt(1000)).toChar() // CJK
                    3 -> (0x03B1 + random.nextInt(25)).toChar() // Greek lowercase
                    else -> (0x2200 + random.nextInt(256)).toChar() // Mathematical Operators
                }
            }
        }
    }

    /**
     * Tokenize the input and return the total number of characters consumed.
     */
    private fun tokenizeAndCountConsumed(input: String): Int {
        val lexer = createLexer()
        lexer.start(input)

        var expectedPosition = 0
        while (lexer.tokenType != null) {
            val tokenStart = lexer.tokenStart
            val tokenEnd = lexer.tokenEnd

            // Verify token bounds are valid
            assertTrue(
                "Token start ($tokenStart) should not be negative", tokenStart >= 0
            )
            assertTrue(
                "Token should start at expected position $expectedPosition but starts at $tokenStart",
                tokenStart == expectedPosition
            )
            assertTrue(
                "Token end ($tokenEnd) should not be before start ($tokenStart)", tokenEnd >= tokenStart
            )
            assertTrue(
                "Token end ($tokenEnd) should not exceed input length (${input.length})", tokenEnd <= input.length
            )

            expectedPosition = tokenEnd
            lexer.advance()
        }

        return expectedPosition
    }
}
