package forgelord.hello

import forgelord.Sources
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Lesson 1 stands on its own: `page/`, LO-TEK and Streamlord, and nothing else of this app. That
 * keeps it short to read, and lets anyone copy the package to start a page of their own.
 */
class HelloBoundaryTest {
    @Test
    fun `hello uses only page from the rest of the app`() {
        val found = Sources.references(Sources.files("hello"), listOf("components", "quests", "ledger", "configuration"))

        assertTrue(found.isEmpty(), "hello/ refers to what it must not:\n" + found.joinToString("\n"))
    }
}
