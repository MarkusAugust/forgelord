package forgelord

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The README's Architecture section draws the folders twice, as a diagram and as a tree. A folder
 * added or removed in the code, and not in the README, fails here. The arrows are not checked: the
 * boundary tests hold those.
 */
class ReadmeTest {
    /** The folders under `forgelord/`, two levels down, except the components inside `components/`. */
    private val folders =
        Sources.root.walkTopDown().maxDepth(2)
            .filter { it.isDirectory && it != Sources.root }
            .map { it.relativeTo(Sources.root).invariantSeparatorsPath }
            .filterNot { it.startsWith("components/") }
            .sorted()
            .toList()

    private val architecture =
        File("README.md").readText()
            .substringAfter("\n## Architecture\n")
            .substringBefore("\n## ")

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
}
