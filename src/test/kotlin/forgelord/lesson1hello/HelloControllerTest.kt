package forgelord.lesson1hello

import forgelord.datastar
import forgelord.dom
import forgelord.requestWithNonce
import forgelord.testStreamlord
import io.github.markusaugust.streamlord.test.datastarEvents
import no.lotek.assertLotek
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.assertj.MockMvcTester
import kotlin.test.Test
import kotlin.test.assertTrue

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

    // Exercise 1 asks the reader to change the greeting's words. So these two check the name and
    // the element, and no word or full stop around them.
    @Test
    fun `a name gets a greeting with the name in it`() {
        greet("""{"name":"Ada"}""").assertPatchElements(containing = "Ada")
    }

    @Test
    fun `no name gets a greeting too`() {
        val answer = mvc.datastar(HttpMethod.POST, "/hello", """{"name":"  "}""").response.contentAsString
        val greeting = dom(answer.substringAfter("data: elements ")).getElementById("greeting")

        assertTrue(greeting?.text().orEmpty().isNotBlank(), "With no name, the greeting still says something")
    }

    private fun greet(signals: String) = datastarEvents(mvc.datastar(HttpMethod.POST, "/hello", signals).response.contentAsString)
}
