package forgelord.together

import forgelord.datastar
import forgelord.testStreamlord
import io.github.markusaugust.streamlord.core.domain.CspNonce
import io.github.markusaugust.streamlord.spring.CspNonceFilter
import no.lotek.assertLotek
import org.jsoup.Jsoup
import org.springframework.http.HttpMethod
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.servlet.assertj.MockMvcTester
import kotlin.test.Test
import kotlin.test.assertEquals

/** Lesson 3 without a server: the page, and what "Ride out" answers and changes. */
class TogetherControllerTest {
    private val controller = TogetherController(testStreamlord())
    private val mvc = MockMvcTester.of(controller)

    @Test
    fun `the page is correct markup`() {
        assertLotek(page())
    }

    @Test
    fun `riding out answers 204, and the page shows the new count`() {
        assertEquals(204, ride("rats-in-the-greywood").response.status)

        assertEquals("One has ridden out.", ridersOn("rats-in-the-greywood"))
    }

    @Test
    fun `a quest that is not on the post is not found`() {
        assertEquals(404, ride("no-such-quest").response.status)
    }

    private fun page() = controller.page(MockHttpServletRequest().apply { setAttribute(CspNonceFilter.ATTRIBUTE, CspNonce.generate()) })

    private fun ride(id: String) = mvc.datastar(HttpMethod.POST, "/together/$id/ride")

    /** The riders text on one quest's card, as a reload would show it. */
    private fun ridersOn(id: String): String = Jsoup.parse(page()).select("#together-$id p").last()!!.text()
}
