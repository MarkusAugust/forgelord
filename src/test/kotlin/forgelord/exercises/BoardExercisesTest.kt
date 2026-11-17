package forgelord.exercises

import forgelord.quests.web.CheckedQuest
import forgelord.quests.web.QuestForm
import forgelord.quests.web.check
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The exercises for lesson 4, the board. Exercise 8 checks the form's rules alone. Exercise 9 runs
 * against the whole app on a real server, with the Ledger behind HTTP, so it passes only when every
 * layer does its part. Remove `@Disabled` from one, run it, and change the code until it passes.
 * The README has the steps, and a solution.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
class BoardExercisesTest {
    /** A client for the running server, set up by Spring Boot. */
    @Autowired
    private lateinit var client: RestTestClient

    @Test
    @Disabled("Exercise 8: Keep the task short")
    fun `what has to be done fits in 300 characters`() {
        val quest = QuestForm(name = "Wolves at the Ford", giver = "Ulf", text = "x".repeat(300), reward = "20")

        assertIs<CheckedQuest.Valid>(quest.check())
        val tooLong = assertIs<CheckedQuest.Invalid>(quest.copy(text = "x".repeat(301)).check())
        assertEquals(listOf("text"), tooLong.faults.map { it.field })
    }

    @Test
    @Disabled("Exercise 9: A name already on the post")
    fun `a quest cannot take the name of one already on the post`() {
        val answer =
            post("/quests", """{"quest":{"name":"a tenth of the wood","giver":"Ulf","text":"Five more.","reward":"30"}}""")
                .expectStatus().isOk
                .returnResult(String::class.java)
                .responseBody

        assertContains(answer.orEmpty(), "is already on the post.")
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
}
