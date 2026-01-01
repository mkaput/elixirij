package dev.murek.elixirij.skills

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import dev.murek.elixirij.lang.ExParserDefinition
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.*
import kotlin.system.exitProcess

private val testFilePath = Paths.get("src/test/kotlin/dev/murek/elixirij/lang/parser/ExParserTest.kt")
private val testDataDir = Paths.get("src/test/testData/parser")

private data class ParseResult(
    val hasErrors: Boolean,
    val tree: String,
)

private data class Candidate(
    val path: Path,
    val text: String,
    val tree: String,
)

private data class Assigned(
    val candidate: Candidate,
    val number: Int,
)

private data class GrillArgs(
    val root: Path,
    val maxNewTests: Int,
)


@Suppress("JUnitMalformedDeclaration")
private class GrillHarness private constructor() : ParsingTestCase("parser", "ex", true, ExParserDefinition()),
    AutoCloseable {

    companion object {
        fun start(): GrillHarness = GrillHarness().also { it.setUp() }
    }

    override fun close() = tearDown()

    override fun getTestDataPath(): String = testDataDir.toString()

    fun parse(text: String, name: String): ParseResult {
        val psiFile = parseFile(name, text)
        val errors = PsiTreeUtil.collectElementsOfType(psiFile, PsiErrorElement::class.java)
        val tree = toParseTreeText(psiFile, skipSpaces(), includeRanges()).trimEnd()
        return ParseResult(errors.isNotEmpty(), tree)
    }
}

fun main(args: Array<String>) {
    val args = try {
        require(testFilePath.exists() && testFilePath.isRegularFile() && testDataDir.exists() && testDataDir.isDirectory()) {
            "Run this script from the project root; test paths not found."
        }

        parseGrillArgs(args)
    } catch (e: Exception) {
        println(e.message)
        exitProcess(2)
    }

    println("Grilling project: ${args.root}")
    println("Will attempt to collect at most ${args.maxNewTests} tests")

    val existingFixtureContents = loadExistingGrillFixtures()
    val candidates = mutableListOf<Candidate>()

    GrillHarness.start().use { harness ->
        Files.walk(args.root).use { paths ->
            paths.filter { it.isRegularFile() && it.isElixirSource }.forEach { path ->
                val text = path.readText()
                if (existingFixtureContents.contains(text)) {
                    return@forEach
                }
                val result = harness.parse(text, path.name)
                if (result.hasErrors) {
                    candidates.add(Candidate(path, text, result.tree))
                }
            }
        }
    }

    if (candidates.size > args.maxNewTests) {
        println("Found ${candidates.size} new parser failures. Limiting to first ${args.maxNewTests}. Tell the user to re-run this skill after fixing the collected tests.")
        candidates.subList(args.maxNewTests, candidates.size).clear()
    }

    if (candidates.isEmpty()) {
        println("No new parser failures found. Aw yeah!")
        exitProcess(0)
    }

    val testSource = testFilePath.readText()
    val nextGrill = (findGrillNumbers(testSource).maxOrNull() ?: 0) + 1
    val assigned = candidates.mapIndexed { index, candidate ->
        Assigned(candidate, nextGrill + index)
    }

    for (assignment in assigned) {
        val baseName = "grill${assignment.number}"
        val sourcePath = testDataDir / "$baseName.ex"
        val treePath = testDataDir / "$baseName.txt"
        sourcePath.writeText(assignment.candidate.text.ensureTrailingNewline())
        treePath.writeText(assignment.candidate.tree.ensureTrailingNewline())
    }

    val updated = insertGrillTests(testSource, assigned.map { "testGrill${it.number}" })
    testFilePath.writeText(updated)

    println("Added ${assigned.size} freshly grilled test(s): ${assigned.joinToString { "testGrill${it.number}" }}")
    exitProcess(0)
}

private fun parseGrillArgs(args: Array<String>): GrillArgs {
    require(args.size == 2) { "Usage: <path-to-project-root-or-subdir> <max-tests>" }

    val root = args[0].let { expandUserHome(it).normalize().absolute() }
        .also { require(it.exists() && it.isDirectory()) { "Path to project root must be an existing directory: ${args[0]}" } }

    val maxNewTests = args[1].toIntOrNull()?.takeIf { it > 0 }
        .let { requireNotNull(it) { "Invalid max-tests value, must be positive integer: ${args[1]}" } }

    return GrillArgs(root, maxNewTests)
}

private fun expandUserHome(raw: String): Path {
    val s = raw.trim()
    val home by lazy { Path(System.getProperty("user.home")) }
    return when {
        s == "~" -> home
        s.startsWith("~/") -> home / s.removePrefix("~/")
        s.startsWith("~\\") -> home / s.removePrefix("~\\")
        else -> Path(s)
    }
}

private val Path.isElixirSource: Boolean
    get() = extension in setOf("ex", "exs")

private fun loadExistingGrillFixtures(): Set<String> =
    testDataDir.listDirectoryEntries("*.ex").map { it.readText() }.toSet()

private fun findGrillNumbers(source: String): List<Int> =
    Regex("fun\\s+testGrill(\\d+)").findAll(source).map { it.groupValues[1].toInt() }.toList()

private fun insertGrillTests(source: String, testNames: List<String>): String {
    if (testNames.isEmpty()) {
        return source
    }
    val sectionIndex = source.indexOf("// C. Parser Grilling")
    require(sectionIndex >= 0) { "Section C header not found in ExParserTest.kt" }

    val marker =
        "\n    // =============================================================================\n    // 1. Literals"
    val insertIndex = source.indexOf(marker, sectionIndex)
    require(insertIndex >= 0) { "Section C end marker not found in ExParserTest.kt" }

    val insertion = buildString {
        append("\n")
        for (name in testNames) {
            append("    fun $name() = doTest()\n")
        }
        append("\n")
    }

    return source.substring(0, insertIndex) + insertion + source.substring(insertIndex)
}

private fun String.ensureTrailingNewline(): String = trimEnd() + "\n"
