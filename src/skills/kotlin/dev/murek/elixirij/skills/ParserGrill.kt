package dev.murek.elixirij.skills

import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.ParsingTestCase
import dev.murek.elixirij.lang.ExParserDefinition
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
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

internal data class GrillArgs(
    val root: Path,
    val maxNewTests: Int,
)

internal sealed interface GrillArgsResult

internal data class GrillArgsOk(val args: GrillArgs) : GrillArgsResult

internal data class GrillArgsError(val message: String) : GrillArgsResult

private class GrillHarness : ParsingTestCase("parser", "ex", true, ExParserDefinition()) {
    override fun getTestDataPath(): String = testDataDir.toString()

    fun start() = setUp()

    fun stop() = tearDown()

    fun parse(text: String, name: String): ParseResult {
        val psiFile = parseFile(name, text)
        val errors = PsiTreeUtil.collectElementsOfType(psiFile, PsiErrorElement::class.java)
        val tree = toParseTreeText(psiFile, skipSpaces(), includeRanges()).trimEnd()
        return ParseResult(errors.isNotEmpty(), tree)
    }
}

fun main(args: Array<String>) {
    val rootArgs = when (val result = parseGrillArgs(args)) {
        is GrillArgsOk -> result.args
        is GrillArgsError -> {
            System.err.println(result.message)
            exitProcess(2)
        }
    }
    val root = rootArgs.root
    if (!root.exists() || !root.isDirectory()) {
        System.err.println("Root directory not found: $root")
        exitProcess(2)
    }
    if (!testFilePath.exists() || !testDataDir.exists()) {
        System.err.println("Run this script from the project root; test paths not found.")
        exitProcess(2)
    }

    val existingFixtureContents = loadExistingGrillFixtures(testDataDir)
    val candidates = mutableListOf<Candidate>()

    val harness = GrillHarness()
    try {
        harness.start()
        Files.walk(root).use { paths ->
            paths
                .filter { it.isRegularFile() && it.isElixirSource() }
                .forEach { path ->
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
    } finally {
        harness.stop()
    }

    val newCandidates = candidates.sortedBy { it.path.toString() }
    if (newCandidates.size > rootArgs.maxNewTests) {
        System.err.println(
            "Found ${newCandidates.size} new parser failures (limit ${rootArgs.maxNewTests}). Aborting without changes."
        )
        exitProcess(1)
    }

    if (newCandidates.isEmpty()) {
        println("No new parser failures found.")
        exitProcess(0)
    }

    val testSource = testFilePath.readText()
    val nextGrill = (findGrillNumbers(testSource).maxOrNull() ?: 0) + 1
    val assigned = newCandidates.mapIndexed { index, candidate ->
        Assigned(candidate, nextGrill + index)
    }

    assigned.forEach { assignment ->
        val baseName = "grill${assignment.number}"
        val sourcePath = testDataDir.resolve("$baseName.ex")
        val treePath = testDataDir.resolve("$baseName.txt")
        sourcePath.writeText(ensureTrailingNewline(assignment.candidate.text))
        treePath.writeText(ensureTrailingNewline(assignment.candidate.tree))
    }

    val updated = insertGrillTests(testSource, assigned.map { "testGrill${it.number}" })
    testFilePath.writeText(updated)

    println("Added ${assigned.size} smoke test(s): ${assigned.joinToString { "testGrill${it.number}" }}")
    exitProcess(0)
}

private fun resolveRoot(arg: String): Path {
    val raw = arg.ifBlank {
        return Paths.get("").toAbsolutePath().normalize()
    }
    val expanded = if (raw.startsWith("~/")) {
        System.getProperty("user.home") + raw.removePrefix("~")
    } else {
        raw
    }
    return Paths.get(expanded).toAbsolutePath().normalize()
}

internal fun parseGrillArgs(args: Array<String>): GrillArgsResult {
    if (args.size != 2) {
        return GrillArgsError("Usage: <path-to-project-root-or-subdir> <max-tests>")
    }
    val root = resolveRoot(args[0])
    val maxNewTests = args[1].toIntOrNull()?.takeIf { it > 0 }
        ?: return GrillArgsError("Invalid max-tests value: ${args[1]}")
    return GrillArgsOk(GrillArgs(root, maxNewTests))
}

private fun Path.isElixirSource(): Boolean = when (extension) {
    "ex", "exs" -> true
    else -> false
}

private fun loadExistingGrillFixtures(dir: Path): Set<String> {
    if (!dir.isDirectory()) {
        return emptySet()
    }
    return dir.listDirectoryEntries("grill*.ex")
        .map { it.readText() }
        .toSet()
}

private fun findGrillNumbers(source: String): List<Int> {
    val regex = Regex("fun\\s+testGrill(\\d+)")
    return regex.findAll(source).map { it.groupValues[1].toInt() }.toList()
}

fun insertGrillTests(source: String, testNames: List<String>): String {
    if (testNames.isEmpty()) {
        return source
    }
    val sectionIndex = source.indexOf("// C. Parser Grilling")
    require(sectionIndex >= 0) { "Section C header not found in ExParserTest.kt" }

    val marker = "\n    // =============================================================================\n    // 1. Literals"
    val insertIndex = source.indexOf(marker, sectionIndex)
    require(insertIndex >= 0) { "Section C end marker not found in ExParserTest.kt" }

    val insertion = buildString {
        append("\n")
        testNames.forEach { name ->
            append("    fun ").append(name).append("() = doTest()\n")
        }
        append("\n")
    }

    return source.substring(0, insertIndex) + insertion + source.substring(insertIndex)
}

private fun ensureTrailingNewline(text: String): String = if (text.endsWith("\n")) text else "$text\n"
