package forgelord

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The README's "Where the code is", in lesson 4, draws the folders twice, as a diagram and as a
 * tree. A folder added or removed in the code, and not in the README, fails here. So does an
 * arrow in the diagram that `ArchitectureTest.MAY_USE` does not have, or the other way round.
 *
 * Each exercise says where its change goes, with a link to the line. A line that has moved fails
 * here too.
 */
class ReadmeTest {
    private val folders = Sources.folders

    private val readme = File("README.md").readText()

    private val architecture =
        readme
            .substringAfter("\n### Where the code is\n")
            .substringBefore("\n### ")

    @Test
    fun `the diagram names every folder`() {
        val diagram = architecture.substringAfter("```mermaid\n").substringBefore("```")
        val named =
            Regex(""""([a-z]+(?:/[a-z]+)?)"""").findAll(diagram)
                .map { it.groupValues[1] }
                .distinct()
                .sorted()
                .toList()

        assertEquals(folders, named, "The diagram in the README and the folders differ")
    }

    @Test
    fun `the diagram's arrows are what each folder uses`() {
        val diagram = architecture.substringAfter("```mermaid\n").substringBefore("```")
        // hello["hello"] gives a box its name. The arrows use the short name before the bracket.
        val names = Regex("""(\w+)\["([a-z/]+)"]""").findAll(diagram).associate { it.groupValues[1] to it.groupValues[2] }
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
            readme.split("\n### ")
                .filter { it.startsWith("Exercise ") }
                .filterNot { "\n**Where:** " in it }
                .map { it.lineSequence().first() }

        assertEquals(emptyList(), silent, "Exercises with no line that starts with **Where:**")
    }

    @Test
    fun `a link to a line points at the name it is written on`() {
        // [`greet`](src/…/HelloController.kt#L61): line 61 of that file has to say greet.
        val moved =
            Regex("""\[`(\w+)`]\((src/[\w/.]+)#L(\d+)\)""").findAll(readme)
                .filterNot { link ->
                    val (name, file, line) = link.destructured
                    File(file).takeIf { it.isFile }?.readLines()?.getOrNull(line.toInt() - 1)?.contains(name) == true
                }.map { it.value }
                .toList()

        assertEquals(emptyList(), moved, "Links in the README whose line does not have the name")
    }
}
