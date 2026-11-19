package forgelord.lesson4quests

import forgelord.lesson4quests.client.QuestClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
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
 *
 * The asking runs in [scope], which is handed in: the app's own in production, and in a test one
 * whose clock the test moves.
 */
@Service
class QuestService(
    private val client: QuestClient,
    @param:Value("\${forgelord.ledger.poll-interval}") private val pollInterval: Duration,
    private val scope: CoroutineScope,
) {
    /** Asks the board to read the Ledger now. Several asks before it gets to them count as one. */
    private val refresh = Channel<Unit>(Channel.CONFLATED)

    val board: StateFlow<QuestResult> =
        flow {
            while (true) {
                emit(load())
                withTimeoutOrNull(pollInterval.toKotlinDuration()) { refresh.receive() }
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), QuestResult.Loading)

    fun nail(quest: NewQuest): WriteResult = client.nail(quest).also { readAgain() }

    fun rideOut(id: String): WriteResult = client.rideOut(id).also { readAgain() }

    /** After every write, whether it went through or not: the Ledger may have changed either way. */
    private fun readAgain() {
        refresh.trySend(Unit)
    }

    private fun load(): QuestResult =
        when (val reading = client.quests()) {
            is Reading.Answered -> if (reading.quests.isEmpty()) QuestResult.Empty else QuestResult.Found(reading.quests)
            Reading.Closed -> QuestResult.Unavailable
        }
}
