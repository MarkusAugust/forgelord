package forgelord

import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Tag
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The README's solutions, pasted in, pass the exercises' tests.
 *
 * This copies the project, puts every solution's code where the README says, switches every
 * exercise on, and runs the copy's tests. The code is taken from the README as it stands, so a
 * solution that does not compile, or does not pass its test, fails here.
 *
 * Where a piece of code goes is written twice in the README: in words, for the reader, and in a
 * comment above the code, for this test. See [Solutions] for what the comment says.
 *
 * It builds the project a second time, which takes half a minute. So it is left out of
 * `./gradlew test` and runs with `./gradlew check`.
 */
@Tag("solutions")
class SolutionsTest {
    @Test
    fun `the solutions in the README, pasted in, pass every exercise`() {
        // The solutions go into the code as it is before any exercise is solved. A reader who has
        // switched an exercise on has changed that code, and is not who this test is for.
        assumeFalse(Solutions.exerciseSwitchedOn(File(".")), "An exercise is switched on, so the code is no longer what the solutions go into")

        val copy = Files.createTempDirectory("forgelord-solutions").toFile()
        try {
            // The sources, and nothing a build or an editor leaves behind. `.github` has the
            // instruction files, which a test reads.
            File(".").listFiles().orEmpty()
                .filterNot { it.name in setOf("build", "bin", "out") || (it.name.startsWith(".") && it.name != ".github") }
                .forEach { it.copyRecursively(File(copy, it.name)) }

            Solutions.steps(File(copy, "README.md").readText()).forEach { it.applyIn(copy) }
            val exercises = File(copy, Solutions.EXERCISES).walk().filter { it.isFile }.toList()
            exercises.forEach { file -> file.writeText(file.readLines().filterNot { Solutions.switchesOff(it) }.joinToString("\n", postfix = "\n")) }

            val gradle = File(copy, if (System.getProperty("os.name").startsWith("Windows")) "gradlew.bat" else "gradlew").apply { setExecutable(true) }
            val log = File(copy, "solutions.log")
            val build = ProcessBuilder(gradle.absolutePath, "test", "--console=plain").directory(copy).redirectErrorStream(true).redirectOutput(log).start()
            assertTrue(build.waitFor(10, TimeUnit.MINUTES), "The copy's tests did not finish in ten minutes")
            // What failed and why, first: the end of the log alone is mostly Spring shutting down.
            val told = log.readLines().let { lines -> lines.filter { " FAILED" in it || it.startsWith("e: ") } + "…" + lines.takeLast(60) }
            assertEquals(0, build.exitValue(), "The tests fail with the README's solutions pasted in:\n" + told.joinToString("\n"))

            // Passing is not enough: every exercise's test has to have run.
            val results = File(copy, "build/test-results/test").listFiles { file -> "ExercisesTest" in file.name }.orEmpty().map { it.readText() }
            val ran = results.sumOf { Regex("""<testsuite [^>]*tests="(\d+)"""").find(it)!!.groupValues[1].toInt() }
            val skipped = results.sumOf { Regex("""<testsuite [^>]*skipped="(\d+)"""").find(it)!!.groupValues[1].toInt() }
            val switchedOff = File(Solutions.EXERCISES).walk().filter { it.isFile }.sumOf { file -> file.readLines().count { Solutions.switchesOff(it) } }
            assertEquals(0, skipped, "Exercise tests that did not run")
            assertTrue(switchedOff > 0, "No exercise test is switched off here, so there was nothing to check")
            assertEquals(switchedOff, ran, "The exercise tests that are switched off here, and those that ran in the copy")
        } finally {
            copy.deleteRecursively()
        }
    }
}

/**
 * Where the README's solution code goes, read from the comments above it.
 *
 * A comment is `<!-- solution: OPERATION FILE :: ANCHOR -->`, on the line above a code block. The
 * file is relative to `src/main/kotlin/forgelord/`, unless it starts with `src/` or is
 * `README.md`. The anchor is text one line of the file has, and only one. `A >> B` is the first
 * line with B after the one line with A: in Kotlin, inside the statement A starts, such as a
 * function. Write `&gt;` for `>` in an anchor, since `-->` would end the comment.
 *
 * - `after`: after the statement that starts on the anchor's line, to its closing brace.
 * - `before`: before the anchor's line, and before the annotations and comments above it.
 * - `replace`: in place of the statement that starts on the anchor's line. When the code starts
 *   with an annotation, the annotations above the anchor go too.
 * - `replace-line`: in place of the anchor's line.
 * - `replace-head`: in place of the line that opens the block the anchor's line is in.
 * - `after-enclosing`: after the block the anchor's line is in.
 * - `inside`: on the line after the anchor, one step further in.
 * - `imports`: with the file's other imports. No anchor.
 * - `new-file`: the whole file. No anchor.
 *
 * Two comments above one block put the block in two places.
 */
object Solutions {
    const val EXERCISES = "src/test/kotlin/forgelord/exercises"

    val OPERATIONS = setOf("after", "before", "replace", "replace-line", "replace-head", "after-enclosing", "inside", "imports", "new-file")

    /** A solution comment, whole. `ReadmeTest` uses it too, so both read a comment the same way. */
    val COMMENT = Regex("""<!-- solution: (\S+) (\S+)(?: :: (.+?))? -->""")

    /** True for the line that switches an exercise's test off. */
    fun switchesOff(line: String) = line.trim().startsWith("@Disabled(\"Exercise ")

    /** True when the reader has switched an exercise on: fewer `@Disabled` than tests. */
    fun exerciseSwitchedOn(project: File): Boolean {
        val lines = File(project, EXERCISES).walk().filter { it.isFile }.flatMap { it.readLines() }.map { it.trim() }.toList()
        return lines.count { it.startsWith("@Test") } != lines.count { it.startsWith("@Disabled") }
    }

    class Step(
        val operation: String,
        val file: String,
        anchor: String,
        val code: String,
    ) {
        val anchor = anchor.replace("&gt;", ">")

        private val describe get() = "<!-- solution: $operation $file${if (anchor.isEmpty()) "" else " :: $anchor"} -->"

        fun applyIn(project: File) {
            val path = if (file.startsWith("src/") || file == "README.md") file else "src/main/kotlin/forgelord/$file"
            val target = File(project, path)
            if (operation == "new-file") {
                check(!target.exists()) { "$describe: $path is there already" }
                target.parentFile.mkdirs()
                target.writeText(code)
                return
            }
            check(target.isFile) { "$describe: there is no $path" }
            val lines = target.readLines().toMutableList()
            val kotlin = path.endsWith(".kt")
            // Kotlin and CSS are moved in to where they land. Anything else, such as the README's tree, is kept as written.
            val indents = kotlin || path.endsWith(".css")
            val step = if (kotlin) "    " else "  "

            fun indentOf(line: Int) = lines[line].takeWhile { it == ' ' }

            fun block(indent: String): List<String> {
                val body = code.trimEnd().lines()
                if (!indents) return body
                val shared = body.filter { it.isNotBlank() }.minOf { line -> line.takeWhile { it == ' ' }.length }
                return body.map { if (it.isBlank()) "" else indent + it.drop(shared) }
            }

            /** The line with its strings and its `//` comment taken out, so a bracket in either is not counted. */
            fun bare(line: Int) = lines[line].replace(Regex(""""(?:\\.|[^"\\])*""""), "\"\"").replace(Regex("""'(?:\\.|[^'\\])'"""), "''").substringBefore("//")

            /** The last line of the statement that starts on [start]: brackets closed, and nothing carrying it on. */
            fun endOf(start: Int): Int {
                var depth = 0
                var line = start
                while (true) {
                    bare(line).forEach {
                        if (it in "({") depth++
                        if (it in ")}") depth--
                    }
                    val goesOn = bare(line).trimEnd().let { it.endsWith("=") || it.endsWith("->") }
                    val next = lines.getOrNull(line + 1)?.trim().orEmpty()
                    if (depth <= 0 && !goesOn && listOf("?:", "?.", ".", "&&", "||").none { next.startsWith(it) }) return line
                    line++
                    check(line < lines.size) { "$describe: the statement at line ${start + 1} of $path never ends" }
                }
            }

            fun find(text: String): Int {
                val parts = text.split(" >> ")
                // A solution comment in the README names its anchor, and is not the line looked for.
                val first = lines.indices.filter { parts.first() in lines[it] && !lines[it].startsWith("<!-- solution:") }
                check(first.size == 1) { "$describe: ${first.size} lines of $path have \"${parts.first()}\", and it has to be one. Say which with A >> B." }
                var at = first.single()
                parts.drop(1).forEach { part ->
                    val last = if (kotlin) endOf(at) else lines.lastIndex
                    at = (at + 1..last).firstOrNull { part in lines[it] } ?: error("$describe: no line of $path has \"$part\" where it was looked for, from line ${at + 2} to ${last + 1}")
                }
                return at
            }

            /** The line that opens the block [line] is in. */
            fun headOf(line: Int): Int {
                var depth = 0
                for (above in line - 1 downTo 0) {
                    bare(above).reversed().forEach {
                        if (it == '}') depth++
                        if (it == '{') depth--
                    }
                    if (depth < 0) return above
                }
                error("$describe: line ${line + 1} of $path is in no block")
            }

            /** The first of the lines right above [line] that start with one of [starts], or [line]. */
            fun withAbove(
                line: Int,
                starts: List<String>,
            ): Int {
                var first = line
                while (first > 0 && lines[first - 1].trim().let { above -> starts.any { above.startsWith(it) } }) first--
                return first
            }

            when (operation) {
                "imports" -> {
                    val last = lines.indexOfLast { it.startsWith("import ") }
                    check(last >= 0) { "$describe: $path has no imports to put these with" }
                    lines.addAll(last + 1, code.trim().lines())
                }
                "after" -> {
                    val start = find(anchor)
                    val end = endOf(start)
                    lines.addAll(end + 1, (if (end > start && indents) listOf("") else emptyList()) + block(indentOf(start)))
                }
                "before" -> {
                    val at = find(anchor)
                    lines.addAll(withAbove(at, listOf("@", "*", "/**", "//")), block(indentOf(at)) + if (indents) listOf("") else emptyList())
                }
                "replace" -> {
                    val start = find(anchor)
                    val end = endOf(start)
                    val first = if (code.trimStart().startsWith("@")) withAbove(start, listOf("@")) else start
                    val new = block(indentOf(start))
                    repeat(end - first + 1) { lines.removeAt(first) }
                    lines.addAll(first, new)
                }
                "replace-line" -> {
                    val at = find(anchor)
                    val new = block(indentOf(at))
                    lines.removeAt(at)
                    lines.addAll(at, new)
                }
                "replace-head" -> {
                    val head = headOf(find(anchor))
                    val new = block(indentOf(head))
                    lines.removeAt(head)
                    lines.addAll(head, new)
                }
                "after-enclosing" -> {
                    val head = headOf(find(anchor))
                    lines.addAll(endOf(head) + 1, block(indentOf(head)))
                }
                "inside" -> {
                    val at = find(anchor)
                    lines.addAll(at + 1, block(indentOf(at) + step))
                }
                else -> error("$describe: no such operation")
            }
            target.writeText(lines.joinToString("\n", postfix = "\n"))
        }
    }

    /** Every solution step in [readme], in the order the README has them. */
    fun steps(readme: String): List<Step> {
        val lines = readme.lines()
        val steps = mutableListOf<Step>()
        var line = 0
        while (line < lines.size) {
            if (!lines[line].startsWith("<!-- solution:")) {
                line++
                continue
            }
            val found =
                generateSequence(line) { it + 1 }.takeWhile { lines[it].startsWith("<!-- solution:") }.map { at ->
                    COMMENT.matchEntire(lines[at])?.takeIf { it.groupValues[1] in OPERATIONS } ?: error("Line ${at + 1} of the README is no solution comment: ${lines[at]}")
                }.toList()
            line += found.size
            check(lines[line].startsWith("```")) { "Line ${line + 1} of the README: a solution comment has to stand right above its code block" }
            val end = (line + 1 until lines.size).first { lines[it].startsWith("```") }
            val code = lines.subList(line + 1, end).joinToString("\n", postfix = "\n")
            line = end + 1
            found.forEach { steps += Step(it.groupValues[1], it.groupValues[2], it.groupValues[3], code) }
        }
        return steps
    }
}
