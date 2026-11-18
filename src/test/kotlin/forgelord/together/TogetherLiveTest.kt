package forgelord.together

import forgelord.datastar
import io.github.markusaugust.streamlord.test.LiveDatastarStream
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.servlet.client.RestTestClient
import java.net.URI
import kotlin.test.Test

/** Lesson 3 on a real server: two windows have the stream open, one rides out, and both see it. */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
class TogetherLiveTest {
    @LocalServerPort
    private var port: Int = 0

    @Autowired
    private lateinit var client: RestTestClient

    @Test
    fun `both windows see a ride`() =
        // Unit, said out loud: JUnit skips a test method that returns anything else, silently.
        runBlocking<Unit> {
            val stream = URI("http://localhost:$port/together/board")
            LiveDatastarStream.open(stream).use { first ->
                LiveDatastarStream.open(stream).use { second ->
                    first.awaitPatchElements(selector = "#$TOGETHER_BOARD_ID", containing = "What the River Carries")
                    second.awaitPatchElements(selector = "#$TOGETHER_BOARD_ID", containing = "What the River Carries")

                    client.datastar("/together/what-the-river-carries/ride").expectStatus().isNoContent

                    first.awaitPatchElements(selector = "#$TOGETHER_BOARD_ID", containing = "One has ridden out.")
                    second.awaitPatchElements(selector = "#$TOGETHER_BOARD_ID", containing = "One has ridden out.")
                }
            }
        }
}
