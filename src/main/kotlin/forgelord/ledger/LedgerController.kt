package forgelord.ledger

import forgelord.page.fragment
import io.github.markusaugust.streamlord.core.application.Streamlord
import io.github.markusaugust.streamlord.core.domain.ElementPatchMode
import io.github.markusaugust.streamlord.spring.cspNonce
import io.github.markusaugust.streamlord.spring.datastarStream
import io.github.markusaugust.streamlord.spring.readSignals
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

/**
 * The Ledger's two faces: a JSON API under /ledger/quests, which the board calls as if the Ledger
 * were another service, and the page at /ledger with its switch.
 */
@RestController
class LedgerController(
    private val ledger: Ledger,
    private val streamlord: Streamlord,
) {
    @GetMapping("/ledger/quests")
    fun entries(): ResponseEntity<List<LedgerEntry>> =
        ledger.entries()?.let { ResponseEntity.ok(it) } ?: ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()

    @PostMapping("/ledger/quests")
    fun add(
        @RequestBody entry: NewLedgerEntry,
    ): ResponseEntity<LedgerEntry> =
        ledger.add(entry)?.let { ResponseEntity.status(HttpStatus.CREATED).body(it) }
            ?: ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()

    @PostMapping("/ledger/quests/{id}/riders")
    fun addRider(
        @PathVariable id: String,
    ): ResponseEntity<Void> =
        when (ledger.addRider(id)) {
            Outcome.DONE -> ResponseEntity.noContent().build()
            Outcome.CLOSED -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
            Outcome.UNKNOWN -> ResponseEntity.notFound().build()
        }

    @GetMapping("/ledger", produces = [MediaType.TEXT_HTML_VALUE])
    fun page(request: HttpServletRequest): String = ledgerPage(request.cspNonce, ledger.describe())

    /** Open, wipe or close, and answer with what the Ledger is now. */
    @PostMapping("/ledger/{action:open|wipe|close}")
    fun switch(
        @PathVariable action: String,
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): StreamingResponseBody {
        // Read, and thrown away, so a request that is not Datastar's JSON is refused. See ViewController.
        request.readSignals(streamlord)
        when (action) {
            "open" -> ledger.open()
            "wipe" -> ledger.wipe()
            else -> ledger.close()
        }
        val state = ledger.describe()
        return response.datastarStream(streamlord, request = request) {
            patchElements(fragment { ledgerState(state) }, selector = "#$LEDGER_STATE_ID", mode = ElementPatchMode.INNER)
        }
    }
}
