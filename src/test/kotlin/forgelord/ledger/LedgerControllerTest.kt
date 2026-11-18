package forgelord.ledger

import forgelord.testStreamlord
import org.springframework.test.web.servlet.assertj.MockMvcTester
import kotlin.test.Test
import kotlin.test.assertEquals

/** The Ledger's JSON API, as the board meets it: a status code for each outcome. */
class LedgerControllerTest {
    private val ledger = Ledger()
    private val mvc = MockMvcTester.of(LedgerController(ledger, testStreamlord()))

    @Test
    fun `a rider is added with POST and taken off with DELETE`() {
        assertEquals(204, mvc.post().uri(RIDERS).exchange().response.status)
        assertEquals(1, riders())

        assertEquals(204, mvc.delete().uri(RIDERS).exchange().response.status)
        assertEquals(0, riders())
    }

    @Test
    fun `a quest the Ledger does not have is 404, and a closed Ledger is 503`() {
        assertEquals(404, mvc.delete().uri("/ledger/quests/no-such-quest/riders").exchange().response.status)

        ledger.close()
        assertEquals(503, mvc.delete().uri(RIDERS).exchange().response.status)
        assertEquals(503, mvc.post().uri(RIDERS).exchange().response.status)
    }

    private fun riders(): Int? = ledger.entries()?.first { it.id == "rats-in-the-greywood" }?.riders

    private companion object {
        const val RIDERS = "/ledger/quests/rats-in-the-greywood/riders"
    }
}
