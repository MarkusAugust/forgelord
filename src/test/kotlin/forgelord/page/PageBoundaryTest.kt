package forgelord.page

import forgelord.Sources
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * `page/` is the same for every page, so it knows nothing about what is on one: no domain, no
 * configuration, and no components. Components may use `page/`; never the other way round.
 */
class PageBoundaryTest {
    @Test
    fun `page knows nothing about the domain, the configuration or the components`() {
        val found = Sources.references(Sources.files("page"), listOf("quests", "ledger", "configuration", "components", "hello", "notice", "together"))

        assertTrue(found.isEmpty(), "page/ refers to what it must not:\n" + found.joinToString("\n"))
    }
}
