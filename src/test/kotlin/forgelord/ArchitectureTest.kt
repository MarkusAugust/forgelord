package forgelord

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What each folder uses of the others, written once, in [MAY_USE]. The README's diagram draws the
 * same arrows, and `ReadmeTest` holds the two together.
 *
 * A folder may use what its row names and nothing else, so a new folder can use nothing, and
 * nothing can use it, until the table says so. The reasons behind the rows:
 *
 * - `hello` and `notice` use `page` alone, so each can be read and copied on its own. `together`
 *   adds `components`. No lesson uses another.
 * - `page` uses nothing, and `components` only `page`, so both can go into another app.
 * - Nothing uses `ledger`. The board reaches it over HTTP, as it would any other service.
 * - `quests/web` uses `quests`, never the other way round, and never `quests/client`.
 * - `configuration` uses nothing, and nothing uses it by name: Spring hands its beans out.
 */
class ArchitectureTest {
    @Test
    fun `every folder has a row, and every row a folder`() {
        assertEquals(Sources.folders, MAY_USE.keys.sorted(), "MAY_USE and the folders under ${Sources.root} differ")
    }

    @Test
    fun `no folder uses what its row does not name`() {
        val found =
            MAY_USE.flatMap { (folder, allowed) ->
                Sources.uses(folder).filter { it.folder !in allowed }.map { "$folder/ may not use ${it.folder}/: ${it.where}" }
            }

        assertTrue(found.isEmpty(), found.joinToString("\n"))
    }

    @Test
    fun `no row names what its folder does not use`() {
        val found =
            MAY_USE.flatMap { (folder, allowed) ->
                val used = Sources.uses(folder).map { it.folder }.toSet()
                (allowed - used).map { "$folder/ no longer uses $it/. Take it out of MAY_USE, and the arrow out of the README." }
            }

        assertTrue(found.isEmpty(), found.joinToString("\n"))
    }

    companion object {
        val MAY_USE: Map<String, Set<String>> =
            mapOf(
                "hello" to setOf("page"),
                "notice" to setOf("page"),
                "together" to setOf("components", "page"),
                "quests/web" to setOf("quests", "components", "page"),
                "quests" to setOf("quests/client"),
                "quests/client" to setOf("quests"),
                "components" to setOf("page"),
                "page" to emptySet(),
                "configuration" to emptySet(),
                "ledger" to setOf("components", "page"),
            )
    }
}
