package forgelord.together

import forgelord.Sources
import kotlin.test.Test
import kotlin.test.assertTrue

/** Lesson 3 adds `components/` to what lesson 2 uses, and still nothing of the other lessons. */
class TogetherBoundaryTest {
    @Test
    fun `together uses only page and components from the rest of the app`() {
        val found =
            Sources.references(
                Sources.files("together"),
                listOf("quests", "ledger", "configuration", "hello", "notice"),
            )

        assertTrue(found.isEmpty(), "together/ refers to what it must not:\n" + found.joinToString("\n"))
    }
}
