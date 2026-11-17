package forgelord.quests

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * What the Ledger answers, as the board's four states, and when the board asks.
 *
 * The service runs in the test's own scope, so two seconds between asks take no time here, and
 * the test says when they have passed. A request the test did not expect ends the test within
 * [LIMIT], with what was asked for.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestServiceTest {
    private val ledger = LedgerStub()

    @Test
    fun `the board starts loading, before anyone asks`() =
        runTest(timeout = LIMIT) {
            assertEquals(QuestResult.Loading, service().board.value)
        }

    @Test
    fun `quests are found`() =
        runTest(timeout = LIMIT) {
            ledger.answers(LedgerStub.QUESTS)

            val result = service().board.first { it != QuestResult.Loading }

            assertEquals("Cut Ropes", (result as QuestResult.Found).quests.single().name)
        }

    @Test
    fun `no quests is empty, not found`() =
        runTest(timeout = LIMIT) {
            ledger.answers("[]")

            assertEquals(QuestResult.Empty, service().board.first { it != QuestResult.Loading })
        }

    @Test
    fun `a closed Ledger is a state, not an exception`() =
        runTest(timeout = LIMIT) {
            ledger.server.expect(requestTo(QUESTS_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

            assertEquals(QuestResult.Unavailable, service().board.first { it != QuestResult.Loading })
        }

    @Test
    fun `a bug while reading is a state too, and the board lives on`() =
        runTest(timeout = LIMIT) {
            ledger.server.expect(requestTo(QUESTS_URL)).andRespond { error("Not the Ledger's doing") }
            ledger.answers("[]")
            val service = service()
            val page = launch { service.board.collect {} }

            runCurrent()
            assertEquals(QuestResult.Unavailable, service.board.value)

            advanceTimeBy(POLL_INTERVAL)
            runCurrent()
            assertEquals(QuestResult.Empty, service.board.value)
            page.cancelAndJoin()
        }

    @Test
    fun `a refused ride is refused, and nothing is thrown`() =
        runTest(timeout = LIMIT) {
            ledger.server.expect(requestTo(RIDERS_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

            assertEquals(WriteResult.REFUSED, service().rideOut("cut-ropes"))
        }

    @Test
    fun `the board asks again every poll interval, and not before`() =
        runTest(timeout = LIMIT) {
            ledger.answers("[]")
            ledger.answers(LedgerStub.QUESTS)
            val service = service()
            val page = launch { service.board.collect {} }

            advanceTimeBy(POLL_INTERVAL - 1.milliseconds)
            runCurrent()
            assertEquals(QuestResult.Empty, service.board.value)

            advanceTimeBy(1.milliseconds)
            runCurrent()
            assertEquals("Cut Ropes", (service.board.value as QuestResult.Found).quests.single().name)
            page.cancelAndJoin()
        }

    @Test
    fun `the board is read again at once after a write`() =
        runTest(timeout = LIMIT) {
            ledger.answers("[]")
            ledger.server.expect(requestTo(RIDERS_URL)).andExpect(method(HttpMethod.POST)).andRespond(withStatus(HttpStatus.NO_CONTENT))
            ledger.answers(LedgerStub.QUESTS)
            val service = service()
            val page = launch { service.board.collect {} }
            runCurrent()

            assertEquals(WriteResult.DONE, service.rideOut("cut-ropes"))

            // No time has passed: the second read came from the write, not from the clock.
            runCurrent()
            assertEquals("Cut Ropes", (service.board.value as QuestResult.Found).quests.single().name)
            page.cancelAndJoin()
        }

    @Test
    fun `when the last page closes, the asking stops and the board is loading again`() =
        runTest(timeout = LIMIT) {
            ledger.answers(LedgerStub.QUESTS)
            val service = service()
            val page = launch { service.board.collect {} }
            runCurrent()

            page.cancelAndJoin()
            advanceTimeBy(POLL_INTERVAL * 3)
            runCurrent()

            assertEquals(QuestResult.Loading, service.board.value)
            // The Ledger was asked once. A second ask would have had no answer waiting.
            ledger.server.verify()
        }

    /** The service, asking in the background of the running test. */
    private fun TestScope.service() = QuestService(ledger.client, Duration.ofSeconds(POLL_INTERVAL.inWholeSeconds), backgroundScope)

    private fun LedgerStub.answers(json: String) {
        server.expect(requestTo(QUESTS_URL)).andRespond(withSuccess(json, MediaType.APPLICATION_JSON))
    }

    private companion object {
        const val QUESTS_URL = "http://ledger.test/ledger/quests"
        const val RIDERS_URL = "http://ledger.test/ledger/quests/cut-ropes/riders"
        val POLL_INTERVAL = 2.seconds

        /** Real time. Nothing here waits, so only a test that is stuck gets this far. */
        val LIMIT = 5.seconds
    }
}
