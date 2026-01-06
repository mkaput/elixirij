package dev.murek.elixirij.ide.highlighting

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ExSyntaxHighlighterTest : BasePlatformTestCase() {

    fun `test interpolation tokens are highlighted in strings`() {
        doTest(
            """
            "<info textAttributesKey="ELIXIR_STRING">a </info><info textAttributesKey="ELIXIR_BRACES">#{</info>b<info textAttributesKey="ELIXIR_BRACES">}</info><info textAttributesKey="ELIXIR_STRING"> c</info>"
            """.trimIndent()
        )
    }

    fun `test interpolation tokens are highlighted in sigils`() {
        doTest(
            """
            ~s(foo <info textAttributesKey="ELIXIR_BRACES">#{</info>bar<info textAttributesKey="ELIXIR_BRACES">}</info>)
            """.trimIndent()
        )
    }

    private fun doTest(code: String) {
        myFixture.configureByText("test.ex", code)
        val document = myFixture.editor.document
        val expected = ExpectedHighlightingData(document, false, false, true, true)
        expected.init()
        PsiDocumentManager.getInstance(project).commitDocument(document)

        val infos = collectHighlightInfos(document.text)
        expected.checkResult(myFixture.file, infos, document.text)
    }

    private fun collectHighlightInfos(text: String): List<HighlightInfo> {
        val highlighter = ExSyntaxHighlighter()
        val lexer = highlighter.highlightingLexer
        lexer.start(text)

        val infos = mutableListOf<HighlightInfo>()
        while (true) {
            val tokenType = lexer.tokenType ?: break
            val highlights = highlighter.getTokenHighlights(tokenType)
            if (highlights.isNotEmpty()) {
                val start = lexer.tokenStart
                val end = lexer.tokenEnd
                for (key in highlights) {
                    infos.add(
                        HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
                            .range(start, end)
                            .textAttributes(key)
                            .createUnconditionally()
                    )
                }
            }
            lexer.advance()
        }
        return infos
    }
}
