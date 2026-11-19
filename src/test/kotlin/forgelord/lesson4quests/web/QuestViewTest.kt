package forgelord.lesson4quests.web

import forgelord.dom
import forgelord.page.checked
import forgelord.lesson4quests.Quest
import forgelord.lesson4quests.QuestResult
import forgelord.testStreamlord
import io.github.markusaugust.streamlord.core.port.driven.toJsonValue
import io.github.markusaugust.streamlord.core.domain.CspNonce
import no.lotek.assertLotek
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/** The page and each of the board's four states, checked against LO-TEK and Datastar. */
class QuestViewTest {
    private val signals = testStreamlord().codec.toJsonValue(QuestSignals())

    @Test
    fun `the page is correct markup`() {
        assertLotek(questPage(CspNonce.generate(), signals))
    }

    @Test
    fun `each state of the board is correct markup, and says what it is`() {
        val quest = Quest("cut-ropes", "Cut Ropes", "Ulf", "Six ropes.", 45, 1)

        assertContains(checked { boardView(QuestResult.Loading) }, "Reading the Ledger.")
        assertContains(checked { boardView(QuestResult.Empty) }, "The post is bare.")
        assertContains(checked { boardView(QuestResult.Unavailable) }, "The Ledger is closed.")
        assertEquals("One has ridden out.", dom(checked { boardView(QuestResult.Found(listOf(quest))) }).select("#quest-cut-ropes p").last()?.text())
    }

    @Test
    fun `the form binds each field to the signal of that name, and to no other`() {
        val bound =
            dom(checked { questForm(signals) }).select("input, textarea").map { field ->
                field.attributes().first { it.key.startsWith("data-bind") }.let { it.key.substringAfter("data-bind:", it.value) }
            }

        // The names as the class has them. A bind to a name the class lacks is a field the server never reads.
        assertEquals(QuestForm::class.java.declaredFields.map { "quest.${it.name}" }, bound)
    }

    @Test
    fun `the form with every field wrong is correct markup`() {
        val html = checked { questForm(signals, (QuestForm().check() as CheckedQuest.Invalid).faults) }

        assertContains(html, "4 things are wrong")
        assertEquals(4, dom(html).select("[aria-invalid=true]").size)
    }
}
