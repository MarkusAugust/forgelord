package forgelord.exercises

import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/**
 * The exercises for lesson 2, the board. They run against the whole app on a real server, with
 * the Ledger behind HTTP, so they pass only when every layer does its part. Remove `@Disabled`
 * from one, run it, and change the code until it passes. The README has the steps, and a solution.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
class BoardExercisesTest {
    /** A client for the running server, set up by Spring Boot. */
    @Autowired
    private lateinit var client: RestTestClient

    @Test
    @Disabled("Exercise 5: A name already on the post")
    fun `a quest cannot take the name of one already on the post`() {
        val answer =
            post("/quests", """{"quest":{"name":"a tenth of the wood","giver":"Ulf","text":"Five more.","reward":"30"}}""")
                .expectStatus().isOk
                .returnResult(String::class.java)
                .responseBody

        assertContains(answer.orEmpty(), "is already on the post.")
    }

    @Test
    @Disabled("Exercise 6: Turn back")
    fun `a rider can turn back, but riders never go below none`() {
        val quest = "/quests/what-the-river-carries"
        post("$quest/ride")
        post("$quest/ride")

        post("$quest/turn-back").expectStatus().isNoContent
        assertEquals(1, riders("what-the-river-carries"))

        post("$quest/turn-back")
        post("$quest/turn-back")
        assertEquals(0, riders("what-the-river-carries"))
    }

    /** A post as Datastar sends one: the signals as JSON, with the Datastar header. */
    private fun post(
        path: String,
        signals: String = "{}",
    ): RestTestClient.ResponseSpec =
        client
            .post()
            .uri(path)
            .header("Datastar-Request", "true")
            .contentType(MediaType.APPLICATION_JSON)
            .body(signals)
            .exchange()

    /** A quest as the Ledger lists it, with only the fields this test reads. */
    private data class LedgerQuest(
        val id: String,
        val riders: Int,
    )

    /** How many riders the Ledger has for [id], asked the way the board asks it. */
    private fun riders(id: String): Int =
        client
            .get()
            .uri("/ledger/quests")
            .exchange()
            .expectStatus().isOk
            .returnResult(object : ParameterizedTypeReference<List<LedgerQuest>>() {})
            .responseBody
            .orEmpty()
            .single { it.id == id }
            .riders
}
