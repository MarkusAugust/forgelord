package forgelord.lesson2notice

import forgelord.page.fragment
import forgelord.page.page
import io.github.markusaugust.streamlord.core.application.Streamlord
import io.github.markusaugust.streamlord.html.dataOnClick
import io.github.markusaugust.streamlord.html.post
import io.github.markusaugust.streamlord.spring.cspNonce
import io.github.markusaugust.streamlord.spring.datastarStream
import io.github.markusaugust.streamlord.spring.readSignals
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.article
import kotlinx.html.button
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.id
import kotlinx.html.main
import kotlinx.html.p
import no.lotek.ButtonVariant
import no.lotek.HeadingSize
import no.lotek.Lt
import no.lotek.Size
import no.lotek.html.lt
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lesson 2: a list from data, and a command.
 *
 * 1. GET /notice sends the page, with one card for each quest in [NOTICES].
 * 2. "Ride out" posts to /notice/{id}/ride. The `{id}` in the path is the quest's id.
 * 3. The server adds one to that quest's count, and answers with the quest's card, made again.
 * 4. Datastar replaces the card that has the same id.
 *
 * Only the window that clicked gets the new card. Another window shows the old count until it
 * reloads. Lesson 3 changes that.
 */
@RestController
class NoticeController(
    private val streamlord: Streamlord,
) {
    /** How many have ridden out on each quest, by the quest's id, kept in memory until the app stops. */
    private val riders: Map<String, AtomicInteger> = NOTICES.associate { it.id to AtomicInteger() }

    @GetMapping("/notice", produces = [MediaType.TEXT_HTML_VALUE])
    fun page(request: HttpServletRequest): String =
        page("The notice board", request.cspNonce, css = noticePageCss) {
            main {
                h1 { lt(Lt.heading(size = HeadingSize.XL)); +"The notice board" }
                for (notice in NOTICES) {
                    noticeCard(notice, riders.getValue(notice.id).get())
                }
            }
        }

    @PostMapping("/notice/{id}/ride")
    fun rideOut(
        @PathVariable id: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): StreamingResponseBody {
        // The command needs no signals, but reading them refuses a request that is not JSON. An
        // HTML form on another site can send form data or plain text, never JSON.
        request.readSignals(streamlord)

        val notice = NOTICES.find { it.id == id } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        val count = riders.getValue(id).incrementAndGet()

        return response.datastarStream(streamlord, request = request) {
            patchElements(fragment { noticeCard(notice, count) })
        }
    }
}

/** A quest on the notice board. */
data class Notice(
    val id: String,
    val name: String,
    val askedBy: String,
)

/** The quests, written in the code. Lesson 4 reads them from another service instead. */
val NOTICES =
    listOf(
        Notice("rats-in-the-greywood", "Rats in the Greywood", "A Sergeant of the Unmourned"),
        Notice("a-tenth-of-the-wood", "A Tenth of the Wood", "The Bridge-Captain"),
        Notice("what-the-river-carries", "What the River Carries", "A River-Priest of Thurn"),
    )

/** One quest's card. Its id comes from the quest's id, so the answer to "Ride out" finds it. */
private fun FlowContent.noticeCard(
    notice: Notice,
    riders: Int,
) {
    article {
        id = "notice-${notice.id}"
        lt(Lt.card())
        h2 { lt(Lt.card.title); +notice.name }
        p { lt(Lt.paragraph(size = Size.SMALL)); +"Asked by ${notice.askedBy}" }
        p { lt(Lt.paragraph()); +ridersText(riders) }
        button {
            lt(Lt.button(variant = ButtonVariant.SECONDARY))
            type = ButtonType.button
            dataOnClick(post("/notice/${notice.id}/ride"))
            +"Ride out"
        }
    }
}

private fun ridersText(count: Int): String =
    when (count) {
        0 -> "Nobody has ridden out yet."
        1 -> "One has ridden out."
        else -> "$count have ridden out."
    }
