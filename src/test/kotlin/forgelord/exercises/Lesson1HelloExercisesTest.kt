package forgelord.exercises

import forgelord.datastar
import forgelord.lesson1hello.HelloController
import forgelord.requestWithNonce
import forgelord.testStreamlord
import org.junit.jupiter.api.Disabled
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.assertj.MockMvcTester
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * The exercises for lesson 1, in lesson1hello/HelloController.kt. Remove `@Disabled` from one, run it,
 * watch it fail, and change the code until it passes. The README has the steps, and a solution.
 */
class Lesson1HelloExercisesTest {
    private val controller = HelloController(testStreamlord())
    private val mvc = MockMvcTester.of(controller)

    @Test
    @Disabled("Exercise 2: Where are you from?")
    fun `the greeting says where you are from`() {
        // Only the name and the place are checked, so the greeting can be in your own words.
        assertContains(greet("""{"name":"Ada","from":"Hookford"}"""), "Ada from Hookford")

        val fromNowhere = greet("""{"name":"Ada","from":""}""")
        assertContains(fromNowhere, "Ada")
        assertFalse(" from " in fromNowhere, "With no place, the greeting says no \"from\"")
    }

    @Test
    @Disabled("Exercise 3: Count the greetings")
    fun `the greetings are counted`() {
        greet("""{"name":"Ada"}""")

        assertContains(greet("""{"name":"Bo"}"""), "Greeting number 2.")
    }

    @Test
    @Disabled("Exercise 4: Show the name as you type")
    fun `the page shows the name as it is typed`() {
        val page = controller.page(requestWithNonce())

        assertContains(page, """data-text="${'$'}name"""")
    }

    /** What the server streams back when the page posts [signals] to /hello. */
    private fun greet(signals: String): String = mvc.datastar(HttpMethod.POST, "/hello", signals).response.contentAsString
}
