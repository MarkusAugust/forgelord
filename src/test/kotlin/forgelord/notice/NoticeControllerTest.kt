package forgelord.notice

import forgelord.datastar
import forgelord.requestWithNonce
import forgelord.testStreamlord
import io.github.markusaugust.streamlord.test.datastarEvents
import no.lotek.assertLotek
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.assertj.MockMvcTester
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/** Lesson 2: the page against LO-TEK, and "Ride out" answered with the quest's card. */
class NoticeControllerTest {
    private val controller = NoticeController(testStreamlord())
    private val mvc = MockMvcTester.of(controller)

    @Test
    fun `the page is correct markup, with a card for each quest`() {
        val page = controller.page(requestWithNonce())

        assertLotek(page)
        for (notice in NOTICES) assertContains(page, """id="notice-${notice.id}"""")
    }

    @Test
    fun `riding out answers with the card, counted`() {
        ride("a-tenth-of-the-wood")

        datastarEvents(ride("a-tenth-of-the-wood").response.contentAsString)
            .assertPatchElements(containing = "2 have ridden out.")
    }

    @Test
    fun `a quest that is not on the board is not found`() {
        assertEquals(404, ride("no-such-quest").response.status)
    }

    private fun ride(id: String) = mvc.datastar(HttpMethod.POST, "/notice/$id/ride")
}
