package forgelord.notice

import forgelord.Sources
import kotlin.test.Test
import kotlin.test.assertTrue

/** Lesson 2 stands on its own, as lesson 1 does: `page/`, LO-TEK and Streamlord, and nothing else of this app. */
class NoticeBoundaryTest {
    @Test
    fun `notice uses only page from the rest of the app`() {
        val found =
            Sources.references(
                Sources.files("notice"),
                listOf("components", "quests", "ledger", "configuration", "hello", "together"),
            )

        assertTrue(found.isEmpty(), "notice/ refers to what it must not:\n" + found.joinToString("\n"))
    }
}
