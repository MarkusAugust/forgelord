package forgelord.exercises

import forgelord.datastar
import forgelord.lesson2notice.NoticeController
import forgelord.testStreamlord
import org.junit.jupiter.api.Disabled
import org.springframework.http.HttpMethod
import org.springframework.test.web.servlet.assertj.MockMvcTester
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

/**
 * The exercise for lesson 2, in lesson2notice/NoticeController.kt. Remove `@Disabled`, run it, watch it
 * fail, and change the code until it passes. The README has the steps, and a solution.
 */
class Lesson2NoticeExercisesTest {
    private val mvc = MockMvcTester.of(NoticeController(testStreamlord()))

    @Test
    @Disabled("Exercise 5: A badge for a popular quest")
    fun `a quest three have ridden out on gets a badge`() {
        ride()
        assertFalse("Popular" in ride(), "Two riders: no badge yet")
        assertContains(ride(), "Popular")
    }

    /** Rides out on one quest, and returns what the server streams back: the card. */
    private fun ride(): String = mvc.datastar(HttpMethod.POST, "/notice/rats-in-the-greywood/ride").response.contentAsString
}
