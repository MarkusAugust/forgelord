package forgelord.quests.client

import forgelord.quests.LedgerStub
import forgelord.quests.NewQuest
import forgelord.quests.Quest
import forgelord.quests.Reading
import forgelord.quests.WriteResult
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import kotlin.test.Test
import kotlin.test.assertEquals

/** The border: the Ledger's JSON and status codes in, the board's own types out. */
class QuestClientTest {
    private val ledger = LedgerStub()

    @Test
    fun `the Ledger's names become the board's`() {
        ledger.server.expect(requestTo(QUESTS_URL))
            .andRespond(withSuccess(LedgerStub.QUESTS, MediaType.APPLICATION_JSON))

        assertEquals(
            Reading.Answered(listOf(Quest("cut-ropes", "Cut Ropes", "Ulf Eightrope", "Six ropes.", 45, 2))),
            ledger.client.quests(),
        )
    }

    @Test
    fun `a new quest is sent in the Ledger's names`() {
        ledger.server.expect(requestTo(QUESTS_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().json("""{"title":"Cut Ropes","askedBy":"Ulf","task":"Six ropes.","marks":45}"""))
            .andRespond(withStatus(HttpStatus.CREATED))

        assertEquals(WriteResult.DONE, ledger.client.nail(NewQuest("Cut Ropes", "Ulf", "Six ropes.", 45)))
        ledger.server.verify()
    }

    @Test
    fun `a closed Ledger is closed, not an exception`() {
        ledger.server.expect(requestTo(QUESTS_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        assertEquals(Reading.Closed, ledger.client.quests())
    }

    @Test
    fun `an answer the board cannot read is closed too`() {
        ledger.server.expect(requestTo(QUESTS_URL)).andRespond(withSuccess("""{"not":"a list"}""", MediaType.APPLICATION_JSON))

        assertEquals(Reading.Closed, ledger.client.quests())
    }

    @Test
    fun `a ride the Ledger takes is done`() {
        ledger.server.expect(requestTo(RIDERS_URL)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.NO_CONTENT))

        assertEquals(WriteResult.DONE, ledger.client.rideOut("cut-ropes"))
    }

    @Test
    fun `a ride on a quest the Ledger does not have is gone`() {
        ledger.server.expect(requestTo(RIDERS_URL)).andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertEquals(WriteResult.GONE, ledger.client.rideOut("cut-ropes"))
    }

    @Test
    fun `a ride while the Ledger is closed is closed`() {
        ledger.server.expect(requestTo(RIDERS_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        assertEquals(WriteResult.CLOSED, ledger.client.rideOut("cut-ropes"))
    }

    @Test
    fun `a status the board has no word for is closed`() {
        ledger.server.expect(requestTo(RIDERS_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertEquals(WriteResult.CLOSED, ledger.client.rideOut("cut-ropes"))
    }

    private companion object {
        const val QUESTS_URL = "http://ledger.test/ledger/quests"
        const val RIDERS_URL = "http://ledger.test/ledger/quests/cut-ropes/riders"
    }
}
