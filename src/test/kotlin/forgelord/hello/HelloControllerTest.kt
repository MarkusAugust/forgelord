package forgelord.hello

import forgelord.datastar
import forgelord.requestWithNonce
import forgelord.testStreamlord
import io.github.markusaugust.streamlord.test.datastarEvents
import no.lotek.assertLotek
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.assertj.MockMvcTester
import kotlin.test.Test

/** Lesson 1, tested the way the board is: the page against LO-TEK, the answer as Datastar events. */
class HelloControllerTest {
    private val controller = HelloController(testStreamlord())
    private val mvc = MockMvcTester.of(controller)

    @Test
    fun `the page is correct markup`() {
        // The nonce CspNonceFilter would have put on the request.
        val request = requestWithNonce()

        assertLotek(controller.page(request))
    }

    @Test
    fun `a name gets a greeting`() {
        greet("""{"name":"Ada"}""").assertPatchElements(containing = "Hello, Ada.")
    }

    @Test
    fun `no name gets a greeting too`() {
        greet("""{"name":"  "}""").assertPatchElements(containing = "Hello, stranger.")
    }

    private fun greet(signals: String) = datastarEvents(mvc.datastar(HttpMethod.POST, "/hello", signals).response.contentAsString)
}
