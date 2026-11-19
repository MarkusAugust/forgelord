package forgelord.exercises

import forgelord.datastar
import forgelord.dom
import forgelord.requestWithNonce
import forgelord.testStreamlord
import forgelord.lesson3together.TogetherController
import org.junit.jupiter.api.Disabled
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.assertj.MockMvcTester
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The exercise for lesson 3, in lesson3together/TogetherController.kt. Remove `@Disabled`, run it, watch
 * it fail, and change the code until it passes. The README has the steps, and a solution.
 */
class Lesson3TogetherExercisesTest {
    private val controller = TogetherController(testStreamlord())
    private val mvc = MockMvcTester.of(controller)

    @Test
    @Disabled("Exercise 6: Turn back")
    fun `a rider can turn back, but riders never go below none`() {
        post("ride")
        post("ride")

        assertEquals(204, post("turn-back"))
        assertEquals("One has ridden out.", riders())

        post("turn-back")
        post("turn-back")
        assertEquals("Nobody has ridden out yet.", riders())
    }

    private val quest = "a-tenth-of-the-wood"

    /** Posts a command for the quest, and returns the status. */
    private fun post(command: String): Int = mvc.datastar(HttpMethod.POST, "/together/$quest/$command").response.status

    /** The riders text on the quest's card, as a reload would show it. */
    private fun riders(): String {
        val page = controller.page(requestWithNonce())
        return dom(page).select("#together-$quest p").last()!!.text()
    }
}
