package forgelord.lesson4quests.web

import io.github.markusaugust.streamlord.test.LiveDatastarStream
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.ServerSocket
import java.net.URI
import kotlin.test.Test

/**
 * A Ledger that takes the call and never answers. Without the timeouts in
 * `application.properties` the board would wait for it for good, on "Reading the Ledger". The
 * test waits out the real read timeout, so it takes a couple of seconds.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
class StalledLedgerTest {
    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `a Ledger that never answers is closed`() =
        // Unit, said out loud: JUnit skips a test method that returns anything else, silently.
        runBlocking<Unit> {
            LiveDatastarStream.open(URI("http://localhost:$port/board")).use { page ->
                page.awaitPatchElements(selector = "#board", containing = "The Ledger is closed.")
            }
        }

    companion object {
        /** A socket that is listening and never read from: the connection opens, and nothing comes back. */
        private val stalled = ServerSocket(0)

        @JvmStatic
        @DynamicPropertySource
        fun ledger(registry: DynamicPropertyRegistry) {
            registry.add("forgelord.ledger.url") { "http://localhost:${stalled.localPort}" }
        }

        @JvmStatic
        @AfterAll
        fun close() = stalled.close()
    }
}
