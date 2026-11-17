package forgelord.components

import forgelord.components.cardlist.CardListClasses
import forgelord.components.cardlist.cardList
import forgelord.components.errorsummary.FieldError
import forgelord.components.errorsummary.errorSummary
import forgelord.components.field.textField
import forgelord.components.shell.shell
import forgelord.components.status.Status
import forgelord.components.status.statusMessage
import forgelord.page.checked
import kotlinx.html.form
import kotlinx.html.p
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The components, each with data that has nothing to do with quests: if one of them only works
 * for the board, it does not belong in `components/`.
 */
class ComponentsTest {
    private data class Relay(
        val code: String,
        val name: String,
    )

    @Test
    fun `a card list has one card per item, with the id the caller gives`() {
        val html = checked { cardList(listOf(Relay("r14", "Relay 14"), Relay("r15", "Relay 15")), id = { "relay-${it.code}" }) { p { +it.name } } }
        val cards = dom(html).select(".${CardListClasses.LIST} > li > article.lt-card")

        assertEquals(listOf("relay-r14", "relay-r15"), cards.map { it.id() })
    }

    @Test
    fun `a field with an error is invalid and says so`() {
        val html = checked { form { textField("callsign", "Callsign", error = "Give a callsign.") } }
        val input = dom(html).getElementById("callsign")

        assertEquals("true", input?.attr("aria-invalid"))
        assertEquals("Give a callsign.", dom(html).select(".lt-error-text").text())
    }

    @Test
    fun `a field without an error hides its error text`() {
        val html = checked { form { textField("callsign", "Callsign", error = null) } }

        assertNotNull(dom(html).selectFirst(".lt-error-text[hidden]"))
        assertNull(dom(html).getElementById("callsign")?.attribute("aria-invalid"))
    }

    @Test
    fun `an error summary links to each field, and hides when there is nothing in it`() {
        val full = checked { form { textField("callsign", "Callsign", "Give a callsign."); errorSummary("errors", listOf(FieldError("callsign", "Give a callsign."))) } }
        val empty = checked { errorSummary("errors", emptyList()) }

        assertEquals("#callsign", dom(full).selectFirst("lt-error-summary a")?.attr("href"))
        assertNotNull(dom(empty).selectFirst("lt-error-summary[hidden]"))
    }

    @Test
    fun `an error is an alert, the other states wait their turn`() {
        assertEquals("alert", dom(checked { statusMessage(Status.ERROR, "Down.") }).selectFirst(".lt-alert")?.attr("role"))
        assertEquals("status", dom(checked { statusMessage(Status.LOADING, "Loading.") }).selectFirst(".lt-alert")?.attr("role"))
    }

    @Test
    fun `a shell holds a title and whatever it is given`() {
        val html = checked { shell("Relays") { p { +"Nothing yet." } } }

        assertEquals("Relays", dom(html).select("h2").text())
        assertEquals("Nothing yet.", dom(html).select("p").text())
    }
}
