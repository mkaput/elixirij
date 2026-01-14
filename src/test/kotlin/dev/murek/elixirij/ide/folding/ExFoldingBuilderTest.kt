package dev.murek.elixirij.ide.folding

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ExFoldingBuilderTest : BasePlatformTestCase() {

    override fun getTestDataPath(): String = "src/test/testData/folding"

    fun `test fold list`() = testFoldingFile("list.ex")

    fun `test fold list single simple`() = testFoldingFile("list_single_simple.ex")

    fun `test fold list with pipe`() = testFoldingFile("list_with_pipe.ex")

    fun `test fold map`() = testFoldingFile("map.ex")

    fun `test fold map update`() = testFoldingFile("map_update.ex")

    // FIXME (issue #36): struct folding not detected; PSI visitor doesn't see ExStruct for this fixture.
//    fun `test fold struct`() = testFoldingFile("struct.ex")

    fun `test fold tuple`() = testFoldingFile("tuple.ex")

    // FIXME (issue #36): bitstring folding not detected; PSI visitor doesn't see ExBitstring for this fixture.
//    fun `test fold bitstring`() = testFoldingFile("bitstring.ex")

    fun `test fold fn`() = testFoldingFile("fn.ex")

    fun `test fold fn with args`() = testFoldingFile("fn_with_args.ex")

    fun `test fold fn multi clause`() = testFoldingFile("fn_multi_clause.ex")

    fun `test fold fn multi`() = testFoldingFile("fn_multi.ex")

    fun `test fold fn complex`() = testFoldingFile("fn_complex.ex")

    fun `test fold do block`() = testFoldingFile("do_block.ex")

    fun `test fold do block multi`() = testFoldingFile("do_block_multi.ex")

    fun `test fold do block complex`() = testFoldingFile("do_block_complex.ex")

    fun `test no fold single line do block`() = testFoldingFile("do_block_single_line.ex")

    fun `test fold heredoc`() = testFoldingFile("heredoc.ex")

    fun `test fold heredoc single line`() = testFoldingFile("heredoc_single_line.ex")

    // FIXME (issue #36): charlist heredoc folding not detected; PSI visitor doesn't see ExCharlistHeredoc for this fixture.
//    fun `test fold charlist heredoc`() = testFoldingFile("charlist_heredoc.ex")

    fun `test fold sigil`() = testFoldingFile("sigil.ex")

    fun `test fold sigil single line`() = testFoldingFile("sigil_single_line.ex")

    fun `test no fold single line list`() = testFoldingFile("list_single_line.ex")

    fun `test fold require series`() = testFoldingFile("require_series.ex")

    fun `test fold alias series`() = testFoldingFile("alias_series.ex")

    fun `test fold import series`() = testFoldingFile("import_series.ex")

    fun `test fold require alias series`() = testFoldingFile("require_alias_series.ex")

    fun `test fold require import series`() = testFoldingFile("require_import_series.ex")

    fun `test fold alias import series`() = testFoldingFile("alias_import_series.ex")

    fun `test fold require alias import series`() = testFoldingFile("require_alias_import_series.ex")

    private fun testFoldingFile(name: String) {
        myFixture.configureByFile(name)
        myFixture.testFolding("$testDataPath/$name")
    }

}
