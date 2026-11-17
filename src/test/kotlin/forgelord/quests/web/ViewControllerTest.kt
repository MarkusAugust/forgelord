package forgelord.quests.web

import forgelord.datastar
import forgelord.quests.LedgerStub
import forgelord.quests.QuestService
import forgelord.testStreamlord
import io.github.markusaugust.streamlord.spring.StreamlordExceptionHandler
import io.github.markusaugust.streamlord.test.datastarEvents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import org.assertj.core.api.Assertions.assertThat
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.servlet.assertj.MockMvcTester
import java.time.Duration
import kotlin.test.AfterTest
import kotlin.test.Test

/** The commands, through the controller, read back as the events Datastar would get. */
class ViewControllerTest {
    private val ledger = LedgerStub()

    // No test here opens the board, so nothing ever runs in the scope.
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val service = QuestService(ledger.client, Duration.ofMinutes(1), scope)

    private val mvc =
        MockMvcTester.of(listOf(ViewController(service, testStreamlord()))) {
            it.setControllerAdvice(StreamlordExceptionHandler()).build()
        }

    @AfterTest
    fun close() = scope.cancel()

    @Test
    fun `an empty quest comes back with what is wrong, and the Ledger is not asked`() {
        val result = mvc.datastar(HttpMethod.POST, "/quests", """{"quest":{}}""")

        assertThat(result).hasStatusOk()
        datastarEvents(result.response.contentAsString).assertPatchElements(containing = "4 things are wrong")
        ledger.server.verify()
    }

    @Test
    fun `a whole quest goes to the Ledger, and the form is emptied`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withStatus(HttpStatus.CREATED))

        val result = mvc.datastar(HttpMethod.POST, "/quests", """{"quest":{"name":"Cut Ropes","giver":"Ulf","text":"Six ropes.","reward":"45"}}""")

        assertThat(result).hasStatusOk()
        datastarEvents(result.response.contentAsString).assertSignal("quest.name", "")
        ledger.server.verify()
    }

    @Test
    fun `a quest the Ledger refuses gets a toast`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        val result = mvc.datastar(HttpMethod.POST, "/quests", """{"quest":{"name":"Cut Ropes","giver":"Ulf","text":"Six ropes.","reward":"45"}}""")

        datastarEvents(result.response.contentAsString).assertPatchElements(containing = "The quest did not go up")
    }

    @Test
    fun `a ride the Ledger takes answers 204 and nothing else`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests/cut-ropes/riders"))
            .andRespond(withStatus(HttpStatus.NO_CONTENT))

        assertThat(mvc.datastar(HttpMethod.POST, "/quests/cut-ropes/ride", "{}")).hasStatus(HttpStatus.NO_CONTENT)
    }

    @Test
    fun `a ride the Ledger refuses gets a toast`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests/cut-ropes/riders"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        val result = mvc.datastar(HttpMethod.POST, "/quests/cut-ropes/ride", "{}")

        datastarEvents(result.response.contentAsString).assertPatchElements(containing = "Nobody rode out")
    }

    @Test
    fun `a ride on a quest the Ledger no longer has gets a toast that says so`() {
        ledger.server.expect(requestTo("http://ledger.test/ledger/quests/cut-ropes/riders"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        val result = mvc.datastar(HttpMethod.POST, "/quests/cut-ropes/ride", "{}")

        datastarEvents(result.response.contentAsString).assertPatchElements(containing = "no longer on the post")
    }

    @Test
    fun `a ride posted as a plain form, as another site could, is refused`() {
        val result =
            mvc.post()
                .uri("/quests/cut-ropes/ride")
                .contentType(MediaType.TEXT_PLAIN)
                .content("x")
                .exchange()

        assertThat(result).hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        ledger.server.verify()
    }
}
