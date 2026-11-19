package forgelord

import org.junit.jupiter.api.Assumptions.assumeFalse
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The README's "Where the code is", in lesson 4, draws the folders twice, as a diagram and as a
 * tree. A folder added or removed in the code, and not in the README, fails here. So does an
 * arrow in the diagram that `ArchitectureTest.MAY_USE` does not have, or the other way round.
 *
 * Each exercise says where its change goes, with a link to the line, and an exercise with a test
 * links to the test. A line that has moved fails here too, and so does an exercise whose number or
 * title is not the one on its test.
 */
class ReadmeTest {
    private val folders = Sources.folders

    private val readme = File("README.md").readText()

    private val architecture =
        readme
            .substringAfter("\n### Where the code is\n")
            .substringBefore("\n### ")

    /** Each exercise in the README, from its heading without the `### ` to the next heading of any kind. */
    private val exerciseParts = readme.split("\n### ").filter { it.startsWith("Exercise ") }.map { it.substringBefore("\n## ") }

    /** The lines of the exercises' tests. */
    private val exerciseTests = File("src/test/kotlin/forgelord/exercises").walk().filter { it.isFile }.flatMap { it.readLines() }.map { it.trim() }.toList()

    /** True when the reader has switched an exercise on. Then lines have moved, and line numbers are not checked. */
    private val solving = exerciseTests.count { it.startsWith("@Test") } != exerciseTests.count { it.startsWith("@Disabled") }

    @Test
    fun `the diagram names every folder`() {
        val diagram = architecture.substringAfter("```mermaid\n").substringBefore("```")
        val named =
            Regex(""""([a-z0-9]+(?:/[a-z0-9]+)?)"""").findAll(diagram)
                .map { it.groupValues[1] }
                .distinct()
                .sorted()
                .toList()

        assertEquals(folders, named, "The diagram in the README and the folders differ")
    }

    @Test
    fun `the diagram's arrows are what each folder uses`() {
        val diagram = architecture.substringAfter("```mermaid\n").substringBefore("```")
        // hello["lesson1hello"] gives a box its name. The arrows use the short name before the bracket.
        val names = Regex("""(\w+)\["([a-z0-9/]+)"]""").findAll(diagram).associate { it.groupValues[1] to it.groupValues[2] }
        val drawn = names.values.associateWith { mutableSetOf<String>() }
        // The dotted arrow to the Ledger is HTTP, not an import, and is left out.
        Regex("""^\s*(\w+) (<)?--> (\w+)\s*$""", RegexOption.MULTILINE).findAll(diagram).forEach {
            val (from, both, to) = it.destructured
            drawn.getValue(names.getValue(from)) += names.getValue(to)
            if (both.isNotEmpty()) drawn.getValue(names.getValue(to)) += names.getValue(from)
        }

        assertEquals<Map<String, Set<String>>>(ArchitectureTest.MAY_USE.toSortedMap(), drawn.toSortedMap(), "The arrows in the README and MAY_USE differ")
    }

    @Test
    fun `the tree names every folder`() {
        val tree = architecture.substringAfter("```text\n").substringBefore("```").lines()
        // Each level of the tree is four characters deep, so the column of ├ or └ gives the level.
        val path = mutableListOf(tree.first().trim())
        val paths =
            tree.drop(1).mapNotNull { line ->
                val column = line.indexOfFirst { it == '├' || it == '└' }
                if (column < 0) return@mapNotNull null
                val level = column / 4 + 1
                while (path.size > level) path.removeLast()
                path += line.substring(column + 4).substringBefore(' ')
                path.joinToString("")
            }
        val named =
            paths
                .filter { it.startsWith("src/main/kotlin/forgelord/") && it.endsWith("/") && it != "src/main/kotlin/forgelord/" }
                .map { it.removePrefix("src/main/kotlin/forgelord/").removeSuffix("/") }
                .sorted()

        assertEquals(folders, named, "The tree in the README and the folders differ")
    }

    @Test
    fun `every exercise says where its change goes`() {
        val silent =
            exerciseParts
                .filterNot { "\n**Where:** " in it }
                .map { it.lineSequence().first() }

        assertEquals(emptyList(), silent, "Exercises with no line that starts with **Where:**")
    }

    @Test
    fun `every switched-off test is an exercise, by number and title`() {
        // @Disabled("Exercise 9: Turn back, on the board") and "### Exercise 9: Turn back, on the board".
        // A new exercise moves the numbers after it, in the README and in the tests.
        val headings = exerciseParts.map { it.lineSequence().first() }
        val strangers = Regex("""@Disabled\("([^"]+)"\)""").findAll(exerciseTests.joinToString("\n")).map { it.groupValues[1] }.filterNot { it in headings }.toList()

        assertEquals(emptyList(), strangers, "Tests switched off with a text that is no exercise in the README")
    }

    @Test
    fun `an exercise's test link points at that exercise's test`() {
        assumeFalse(solving, "An exercise is switched on, so the lines may have moved")

        // **Test:** [`the name`](src/…/Lesson1HelloExercisesTest.kt#L24): line 23 has to switch off this exercise.
        val linked = exerciseParts.filter { "\n**Test:** " in it }
        val wrong =
            linked.filterNot { part ->
                val (file, line) = Regex("""\n\*\*Test:\*\* \[`[^`]+`]\((src/[\w/.]+)#L(\d+)\)""").find(part)?.destructured ?: return@filterNot false
                File(file).takeIf { it.isFile }?.readLines()?.getOrNull(line.toInt() - 2)?.trim() == """@Disabled("${part.lineSequence().first()}")"""
            }.map { it.lineSequence().first() }

        assertEquals(emptyList(), wrong, "The line above the one **Test:** links to has to be @Disabled(\"<the exercise's heading>\"). It is not for")
        assertEquals(exerciseTests.count { it.startsWith("@Disabled") }, linked.size, "The switched-off tests, and the exercises with a **Test:** line, differ in number")
    }

    @Test
    fun `a link to a line points at the name it is written on`() {
        // Solving an exercise moves the lines below it, and the README says so. The lines are
        // checked as long as every exercise is switched off, which is how master is.
        assumeFalse(solving, "An exercise is switched on, so the lines may have moved")

        // [`greet`](src/…/HelloController.kt#L61): line 61 of that file has to say greet.
        val moved =
            Regex("""\[`([^`]+)`]\((src/[\w/.]+)#L(\d+)\)""").findAll(readme)
                .filterNot { link ->
                    val (name, file, line) = link.destructured
                    File(file).takeIf { it.isFile }?.readLines()?.getOrNull(line.toInt() - 1)?.contains(name) == true
                }.map { it.value }
                .toList()

        assertEquals(emptyList(), moved, "Links in the README whose line does not have the name")
    }
}
