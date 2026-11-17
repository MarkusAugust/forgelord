package forgelord.quests.client

import forgelord.quests.LedgerStub
import forgelord.quests.NewQuest
import forgelord.quests.Quest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClientException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The border: the Ledger's JSON in, the board's own types out. */
class QuestClientTest {
    private val ledger = LedgerStub()

    @Test
    fun `the Ledger's names become the board's`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests"))
            .andRespond(withSuccess(LedgerStub.QUESTS, MediaType.APPLICATION_JSON))

        assertEquals(
            listOf(Quest("cut-ropes", "Cut Ropes", "Ulf Eightrope", "Six ropes.", 45, 2)),
            ledger.client.quests(),
        )
    }

    @Test
    fun `a new quest is sent in the Ledger's names`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""{"title":"Cut Ropes","askedBy":"Ulf","task":"Six ropes.","marks":45}"""))
            .andRespond(withStatus(HttpStatus.CREATED))

        ledger.client.nail(NewQuest("Cut Ropes", "Ulf", "Six ropes.", 45))
        ledger.server.verify()
    }

    @Test
    fun `a closed Ledger is an exception the service can catch`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        assertFailsWith<RestClientException> { ledger.client.quests() }
    }
}
