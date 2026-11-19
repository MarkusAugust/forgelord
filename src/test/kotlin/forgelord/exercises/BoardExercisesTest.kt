package forgelord.exercises

import forgelord.datastar
import forgelord.dom
import forgelord.page.fragment
import forgelord.quests.Quest
import forgelord.quests.QuestResult
import forgelord.quests.client.QuestDto
import forgelord.quests.web.CheckedQuest
import forgelord.quests.web.QuestForm
import forgelord.quests.web.boardView
import forgelord.quests.web.check
import org.junit.jupiter.api.Disabled
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.web.servlet.client.RestTestClient
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The exercises for lesson 4, the board. Exercise 10 checks the form's rules alone. Exercises 9 and
 * 11 run against the whole app on a real server, with the Ledger behind HTTP, so they pass only
 * when every layer does its part. Remove `@Disabled` from one, run it, and change the code until
 * it passes. The README has the steps, a hint and a solution.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
class BoardExercisesTest {
    /** A client for the running server, set up by Spring Boot. */
    @Autowired
    private lateinit var client: RestTestClient

    @Test
    @Disabled("Exercise 9: Turn back, on the board")
    fun `a rider can turn back on the board`() {
        val quest = "what-the-river-carries"
        val card = fragment { boardView(QuestResult.Found(listOf(Quest(quest, "What the River Carries", "A River-Priest", "Go north.", 10, riders = 1)))) }
        val button = dom(card).select("button").firstOrNull { it.text() == "Turn back" }
        assertContains(button?.outerHtml().orEmpty(), "/quests/$quest/turn-back", message = "The card has no \"Turn back\" button that posts to turn-back")

        // Every test in this Spring context shares one Ledger, so the count is compared with what it was.
        val before = ridersInTheLedger(quest)
        client.datastar("/quests/$quest/ride").expectStatus().isNoContent
        client.datastar("/quests/$quest/ride").expectStatus().isNoContent

        client.datastar("/quests/$quest/turn-back").expectStatus().isNoContent
        assertEquals(before + 1, ridersInTheLedger(quest))

        val refused =
            client.datastar("/quests/no-such-quest/turn-back")
                .expectStatus().isOk
                .returnResult(String::class.java)
                .responseBody
        assertContains(refused.orEmpty(), "Nobody turned back.")
    }

    @Test
    @Disabled("Exercise 10: Keep the task short")
    fun `what has to be done fits in 300 characters`() {
        val quest = QuestForm(name = "Wolves at the Ford", giver = "Ulf", text = "x".repeat(300), reward = "20")

        assertIs<CheckedQuest.Valid>(quest.check())
        val tooLong = assertIs<CheckedQuest.Invalid>(quest.copy(text = "x".repeat(301)).check())
        assertEquals(listOf(QuestForm::text), tooLong.faults.map { it.field })
    }

    @Test
    @Disabled("Exercise 11: A name already on the post")
    fun `a quest cannot take the name of one already on the post`() {
        val answer =
            client.datastar("/quests", """{"quest":{"name":"a tenth of the wood","giver":"Ulf","text":"Five more.","reward":"30"}}""")
                .expectStatus().isOk
                .returnResult(String::class.java)
                .responseBody

        assertContains(answer.orEmpty(), "is already on the post.")
    }

    /** The count as the Ledger has it, asked directly, and read as the board's client reads it. */
    private fun ridersInTheLedger(quest: String): Int =
        client.get().uri("/ledger/quests").exchange()
            .returnResult(Array<QuestDto>::class.java)
            .responseBody
            .orEmpty()
            .first { it.id == quest }
            .riders
}
