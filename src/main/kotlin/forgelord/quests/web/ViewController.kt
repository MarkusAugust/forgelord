package forgelord.quests.web

import forgelord.components.toast.showDangerToast
import forgelord.page.fragment
import forgelord.quests.QuestService
import forgelord.quests.WriteResult
import io.github.markusaugust.streamlord.core.application.Streamlord
import io.github.markusaugust.streamlord.core.domain.ElementPatchMode
import io.github.markusaugust.streamlord.core.domain.PatchElements
import io.github.markusaugust.streamlord.core.port.driven.encode
import io.github.markusaugust.streamlord.core.port.driven.toJsonValue
import io.github.markusaugust.streamlord.spring.cspNonce
import io.github.markusaugust.streamlord.spring.datastarStream
import io.github.markusaugust.streamlord.spring.readSignals
import io.github.markusaugust.streamlord.spring.readSignalsOr
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The board's page, its stream, and the two commands.
 *
 * A command changes the Ledger and answers only the one who sent it. The stream is the only thing
 * that shows the board, to every page at once.
 */
@RestController
class ViewController(
    private val quests: QuestService,
    private val streamlord: Streamlord,
) {
    /** The form's signals at the start, written by the codec from [QuestSignals]. */
    private val emptySignals = streamlord.codec.toJsonValue(QuestSignals())

    @GetMapping("/", produces = [MediaType.TEXT_HTML_VALUE])
    fun page(request: HttpServletRequest): String = questPage(request.cspNonce, emptySignals)

    /**
     * The board's view: one stream per open page, held open for as long as the page is.
     *
     * `sendLatest` sends the board at once, and again whenever it changes, never more than once
     * every 50 ms, and nothing when the new board is the same as the last.
     */
    @GetMapping("/board")
    fun board(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): StreamingResponseBody =
        response.datastarStream(streamlord, request = request, timeout = Duration.INFINITE) {
            sendLatest(quests.board, minInterval = 50.milliseconds, resumeFrom = request.getHeader("Last-Event-ID")) { result ->
                listOf(PatchElements(fragment { boardView(result) }, selector = "#$BOARD_ID", mode = ElementPatchMode.INNER))
            }
        }

    /**
     * A new quest.
     *
     * Unlike a ride, this answers with markup: the form again, with what was wrong, or empty if
     * the quest went up. Only the sender needs to see what was wrong, and the rules stay on the
     * server. The quest itself reaches every page through the board's stream.
     */
    @PostMapping("/quests")
    fun nail(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): StreamingResponseBody {
        val checked = request.readSignalsOr(QuestSignals(), streamlord).quest.check()

        return response.datastarStream(streamlord, request = request) {
            // The compiler makes both `when`s cover every case.
            when (checked) {
                is CheckedQuest.Invalid -> patchElements(fragment { questForm(emptySignals, checked.faults) })
                is CheckedQuest.Valid ->
                    when (quests.nail(checked.quest)) {
                        WriteResult.DONE -> {
                            patchElements(fragment { questForm(emptySignals) })
                            patchSignals(streamlord.codec.encode(QuestSignals()))
                        }
                        WriteResult.REFUSED ->
                            executeScript(showDangerToast(TOASTS_ID, "The Ledger is closed. The quest did not go up."))
                    }
            }
        }
    }

    /**
     * One more rider. Answers 204 and nothing else when it went through; the new count goes out
     * down the board's stream. When the Ledger refuses, the one who clicked gets a toast.
     */
    @PostMapping("/quests/{id}/ride")
    fun rideOut(
        @PathVariable id: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): StreamingResponseBody? {
        // The signals are not needed, but reading them refuses any request that is not JSON.
        // An HTML form on another site can send form data or plain text, never JSON.
        request.readSignals(streamlord)

        if (quests.rideOut(id) == WriteResult.DONE) {
            response.status = HttpStatus.NO_CONTENT.value()
            return null
        }
        return response.datastarStream(streamlord, request = request) {
            executeScript(showDangerToast(TOASTS_ID, "The Ledger is closed. Nobody rode out."))
        }
    }
}
