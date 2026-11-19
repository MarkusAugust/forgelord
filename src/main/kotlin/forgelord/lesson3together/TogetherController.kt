package forgelord.lesson3together

import forgelord.components.ComponentStyles
import forgelord.components.cardlist.cardList
import forgelord.components.shell.shell
import forgelord.page.fragment
import forgelord.page.page
import io.github.markusaugust.streamlord.core.application.Streamlord
import io.github.markusaugust.streamlord.core.domain.ElementPatchMode
import io.github.markusaugust.streamlord.core.domain.PatchElements
import io.github.markusaugust.streamlord.html.FetchOptions
import io.github.markusaugust.streamlord.html.dataInit
import io.github.markusaugust.streamlord.html.dataOnClick
import io.github.markusaugust.streamlord.html.get
import io.github.markusaugust.streamlord.html.post
import io.github.markusaugust.streamlord.spring.cspNonce
import io.github.markusaugust.streamlord.spring.datastarStream
import io.github.markusaugust.streamlord.spring.readSignals
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h3
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
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

const val TOGETHER_BOARD_ID = "together-board"

/**
 * Lesson 3: every window sees the change.
 *
 * The quests are lesson 2's. What changes is how the new count reaches the page:
 *
 * 1. GET /together sends the page. As it loads, Datastar opens GET /together/board, and the
 *    server keeps that request open. That open request is the stream.
 * 2. "Ride out" posts to /together/{id}/ride. The server changes [riders] and answers
 *    `204 No Content`, with no HTML.
 * 3. Every open stream sees that [riders] changed, and sends its window the list of cards, the
 *    window that clicked included.
 */
@RestController
class TogetherController(
    private val streamlord: Streamlord,
) {
    /**
     * How many have ridden out on each quest, by the quest's id.
     *
     * A `MutableStateFlow` holds one value, here the whole map, and tells every open stream when
     * the value is replaced. The map itself is never changed: [rideOut] puts a
     * new map in its place.
     */
    private val riders = MutableStateFlow(QUESTS.associate { it.id to 0 })

    @GetMapping("/together", produces = [MediaType.TEXT_HTML_VALUE])
    fun page(request: HttpServletRequest): String =
        page("Every window sees it", request.cspNonce, ComponentStyles.css + "\n" + togetherPageCss) {
            main {
                h1 { lt(Lt.heading(size = HeadingSize.XL)); +"Every window sees it" }
                shell("On the post") {
                    div {
                        id = TOGETHER_BOARD_ID
                        // Opens the stream as the page loads. `CLEANUP` closes it when the element
                        // leaves the page. If the stream breaks, Datastar opens it again, up to
                        // 1000 times.
                        dataInit(
                            get("/together/board") {
                                requestCancellation = FetchOptions.RequestCancellation.CLEANUP
                                retryMaxCount = 1000
                            },
                        )
                        cards(riders.value)
                    }
                }
            }
        }

    /**
     * The stream: one per open window, held open as long as the window is.
     *
     * `sendLatest` sends the cards at once, then again each time [riders] gets a new value. It
     * waits at least 50 ms between two sends, and skips a send when the value is the same as the
     * last one sent. `selector` and `INNER` put the cards inside the element with that id.
     */
    @GetMapping("/together/board")
    fun board(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): StreamingResponseBody =
        response.datastarStream(streamlord, request = request, timeout = Duration.INFINITE) {
            sendLatest(riders, minInterval = 50.milliseconds, resumeFrom = request.getHeader("Last-Event-ID")) { counts ->
                listOf(PatchElements(fragment { cards(counts) }, selector = "#$TOGETHER_BOARD_ID", mode = ElementPatchMode.INNER))
            }
        }

    @PostMapping("/together/{id}/ride")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun rideOut(
        @PathVariable id: String,
        request: HttpServletRequest,
    ) {
        // As in lesson 2: reading the signals refuses a request that is not JSON.
        request.readSignals(streamlord)
        if (id !in riders.value) throw ResponseStatusException(HttpStatus.NOT_FOUND)

        // `update` replaces the value: `counts + (id to …)` is a new map with one entry changed.
        riders.update { counts -> counts + (id to counts.getValue(id) + 1) }
    }
}

/** A quest on the post. */
data class Quest(
    val id: String,
    val name: String,
    val askedBy: String,
)

/** Lesson 2's quests, written out again so this lesson can be read on its own. */
val QUESTS =
    listOf(
        Quest("rats-in-the-greywood", "Rats in the Greywood", "A Sergeant of the Unmourned"),
        Quest("a-tenth-of-the-wood", "A Tenth of the Wood", "The Bridge-Captain"),
        Quest("what-the-river-carries", "What the River Carries", "A River-Priest of Thurn"),
    )

/** The cards, from `components/`: `cardList` makes the list, and the code in braces fills each card. */
private fun FlowContent.cards(riders: Map<String, Int>) {
    cardList(QUESTS, cardId = { "together-${it.id}" }) { quest ->
        h3 { lt(Lt.card.title); +quest.name }
        p { lt(Lt.paragraph(size = Size.SMALL)); +"Asked by ${quest.askedBy}" }
        p { lt(Lt.paragraph()); +ridersText(riders.getValue(quest.id)) }
        button {
            lt(Lt.button(variant = ButtonVariant.SECONDARY))
            type = ButtonType.button
            dataOnClick(post("/together/${quest.id}/ride"))
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
