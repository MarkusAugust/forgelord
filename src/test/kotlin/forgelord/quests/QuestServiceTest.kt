package forgelord.quests

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** What the Ledger answers, as the board's four states. */
class QuestServiceTest {
    private val ledger = LedgerStub()

    // Long enough that no second ask comes during a test.
    private val service = QuestService(ledger.client, Duration.ofMinutes(1))

    @AfterTest
    fun close() = service.close()

    @Test
    fun `the board starts loading, before anyone asks`() {
        assertEquals(QuestResult.Loading, service.board.value)
    }

    @Test
    fun `quests are found`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests"))
            .andRespond(withSuccess(LedgerStub.QUESTS, MediaType.APPLICATION_JSON))

        val result = runBlocking { service.board.first { it != QuestResult.Loading } }

        assertEquals("Cut Ropes", (result as QuestResult.Found).quests.single().name)
    }

    @Test
    fun `no quests is empty, not found`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests"))
            .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON))

        assertEquals(QuestResult.Empty, runBlocking { service.board.first { it != QuestResult.Loading } })
    }

    @Test
    fun `a closed Ledger is a state, not an exception`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        assertEquals(QuestResult.Unavailable, runBlocking { service.board.first { it != QuestResult.Loading } })
    }

    @Test
    fun `a refused ride is refused, and nothing is thrown`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests/cut-ropes/riders"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        assertEquals(WriteResult.REFUSED, service.rideOut("cut-ropes"))
    }
}
