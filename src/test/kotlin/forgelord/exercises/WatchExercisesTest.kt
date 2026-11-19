package forgelord.exercises

import forgelord.datastar
import forgelord.dom
import io.github.markusaugust.streamlord.test.LiveDatastarStream
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.servlet.client.RestTestClient
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertContains

/**
 * The exercise for lesson 5: a page of your own. No code for it exists yet, so the test knows the
 * page only by its URLs, and asks a running server for them. Remove `@Disabled`, run it, and
 * build the page until it passes. The README has the recipe, a checklist and a solution.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
class WatchExercisesTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var client: RestTestClient

    @Test
    @Disabled("Exercise 12: The watch")
    fun `a name put on the watch reaches every open window`() =
        // Unit, said out loud: JUnit skips a test method that returns anything else, silently.
        runBlocking<Unit> {
            val page =
                client.get().uri("/watch").exchange()
                    .expectStatus().isOk
                    .returnResult(String::class.java)
                    .responseBody
            val list = dom(page.orEmpty()).getElementById("watch-list")
            assertContains(list?.attr("data-init").orEmpty(), "/watch/list", message = "The page has no element with the id watch-list that opens the stream")

            val stream = URI("http://localhost:$port/watch/list")
            LiveDatastarStream.open(stream).use { first ->
                LiveDatastarStream.open(stream).use { second ->
                    client.datastar("/watch", """{"name":"Ada of the Ford"}""").expectStatus().isNoContent

                    first.awaitPatchElements(selector = "#watch-list", containing = "Ada of the Ford")
                    second.awaitPatchElements(selector = "#watch-list", containing = "Ada of the Ford")
                }
            }
        }
}
