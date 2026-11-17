package forgelord.quests

import forgelord.Sources
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The board reaches the Ledger over HTTP only, as it would reach any other service. That the
 * Ledger runs in the same app is a convenience of this starter, and nothing in `quests/` may
 * lean on it.
 */
class QuestsBoundaryTest {
    @Test
    fun `quests never refers to the Ledger's code`() {
        val found = Sources.references(Sources.files("quests"), listOf("ledger"))

        assertTrue(found.isEmpty(), "quests/ refers to ledger/:\n" + found.joinToString("\n"))
    }
}
