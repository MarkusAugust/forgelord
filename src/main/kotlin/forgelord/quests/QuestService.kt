package forgelord.quests

import forgelord.quests.client.QuestClient
import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClientException
import java.time.Duration
import kotlin.time.toKotlinDuration

/**
 * The board, as the Ledger has it, shared by every page that has it open.
 *
 * [board] asks the Ledger every [pollInterval] while anyone is watching, and at once after a
 * write made here. However many pages are open, the Ledger gets one request at a time. When the
 * last page closes, the asking stops, and the next page starts again from [QuestResult.Loading].
 *
 * Changes made in the Ledger directly reach the board on the next ask. A Ledger that can tell
 * its readers about changes would let this wait for that instead.
 */
@Service
class QuestService(
    private val client: QuestClient,
    @Value("\${forgelord.ledger.poll-interval}") private val pollInterval: Duration,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Asks the board to read the Ledger now. Several asks before it gets to them count as one. */
    private val refresh = Channel<Unit>(Channel.CONFLATED)

    val board: StateFlow<QuestResult> =
        flow {
            while (true) {
                emit(load())
                withTimeoutOrNull(pollInterval.toKotlinDuration()) { refresh.receive() }
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), QuestResult.Loading)

    fun nail(quest: NewQuest): WriteResult = write { client.nail(quest) }

    fun rideOut(id: String): WriteResult = write { client.rideOut(id) }

    @PreDestroy
    fun close() = scope.cancel()

    /** A write, and then the board is read again, whether it went through or not. */
    private fun write(call: () -> Unit): WriteResult {
        val result =
            try {
                call()
                WriteResult.DONE
            } catch (_: RestClientException) {
                WriteResult.REFUSED
            }
        refresh.trySend(Unit)
        return result
    }

    private fun load(): QuestResult =
        try {
            val quests = client.quests()
            if (quests.isEmpty()) QuestResult.Empty else QuestResult.Found(quests)
        } catch (_: RestClientException) {
            QuestResult.Unavailable
        } catch (e: Exception) {
            // Anything else is a bug, not a closed Ledger. Thrown on, it would end the board's
            // flow, and every open page would show "Reading the Ledger" for good.
            log.error("Reading the Ledger failed", e)
            QuestResult.Unavailable
        }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(QuestService::class.java)
    }
}
