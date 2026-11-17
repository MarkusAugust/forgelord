package forgelord.exercises

import forgelord.datastar
import forgelord.hello.HelloController
import forgelord.testStreamlord
import org.junit.jupiter.api.Disabled
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.assertj.MockMvcTester
import kotlin.test.Test
import kotlin.test.assertContains

/**
 * The exercises for lesson 1, in hello/HelloController.kt. Remove `@Disabled` from one, run it,
 * watch it fail, and change the code until it passes. The README has the steps, and a solution.
 */
class HelloExercisesTest {
    private val mvc = MockMvcTester.of(HelloController(testStreamlord()))

    @Test
    @Disabled("Exercise 2: Where are you from?")
    fun `the greeting says where you are from`() {
        assertContains(greet("""{"name":"Ada","from":"Hookford"}"""), "Hello, Ada from Hookford.")
        assertContains(greet("""{"name":"Ada","from":""}"""), "Hello, Ada.")
    }

    @Test
    @Disabled("Exercise 3: Count the greetings")
    fun `the greetings are counted`() {
        greet("""{"name":"Ada"}""")

        assertContains(greet("""{"name":"Bo"}"""), "Greeting number 2.")
    }

    /** What the server streams back when the page posts [signals] to /hello. */
    private fun greet(signals: String): String = mvc.datastar(HttpMethod.POST, "/hello", signals).response.contentAsString
}
