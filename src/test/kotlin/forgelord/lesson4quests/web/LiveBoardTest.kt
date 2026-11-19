package forgelord.lesson4quests.web

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

/**
 * The whole loop on a real server, with the real Ledger behind HTTP: a page has the board open,
 * someone rides out, someone nails up a quest, the Ledger is wiped, closes and opens again, and
 * every change comes down the stream the page already had.
 *
 * This is the one place where the board's JSON meets the Ledger's own, on every route the board
 * uses. The other tests hold the client to JSON written by hand.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = ["forgelord.ledger.poll-interval=100ms"])
@AutoConfigureRestTestClient
class LiveBoardTest {
    /** The port, for the stream: LiveDatastarStream holds a connection of its own. */
    @LocalServerPort
    private var port: Int = 0

    /** A client for the running server, set up by Spring Boot, for the commands. */
    @Autowired
    private lateinit var client: RestTestClient

    @Test
    fun `the board follows the Ledger`() =
        // Unit, said out loud: JUnit skips a test method that returns anything else, silently.
        runBlocking<Unit> {
            LiveDatastarStream.open(URI("http://localhost:$port/board")).use { page ->
                page.awaitPatchElements(selector = "#board", containing = "A Tenth of the Wood")

                post("/quests/a-tenth-of-the-wood/ride")
                page.awaitPatchElements(selector = "#board", containing = "One has ridden out.")

                post("/quests", """{"quest":{"name":"Wolves at the Ford","giver":"Ulf","text":"Six of them.","reward":"20"}}""")
                page.awaitPatchElements(selector = "#board", containing = "Wolves at the Ford")

                post("/ledger/wipe")
                page.awaitPatchElements(selector = "#board", containing = "The post is bare.")

                post("/ledger/close")
                page.awaitPatchElements(selector = "#board", containing = "The Ledger is closed.")

                post("/ledger/open")
                page.awaitPatchElements(selector = "#board", containing = "A Tenth of the Wood")
            }
        }

    private fun post(
        path: String,
        signals: String = "{}",
    ) {
        client.datastar(path, signals).expectStatus().is2xxSuccessful
    }
}
