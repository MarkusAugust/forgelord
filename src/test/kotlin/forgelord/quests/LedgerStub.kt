package forgelord.quests

import forgelord.quests.client.QuestClient
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestClient

/**
 * A [QuestClient] whose Ledger is [server]: each test says what the Ledger answers, and
 * `server.verify()` checks that the board asked for exactly that.
 */
class LedgerStub {
    private val builder = RestClient.builder().baseUrl("http://ledger.test")
    val server: MockRestServiceServer = MockRestServiceServer.bindTo(builder).build()
    val client = QuestClient(builder.build())

    companion object {
        const val QUESTS =
            """[{"id":"cut-ropes","title":"Cut Ropes","askedBy":"Ulf Eightrope","task":"Six ropes.","marks":45,"riders":2}]"""
    }
}
