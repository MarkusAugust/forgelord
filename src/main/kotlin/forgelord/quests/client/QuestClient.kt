package forgelord.quests.client

import forgelord.quests.NewQuest
import forgelord.quests.Reading
import forgelord.quests.WriteResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Lazy
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 * The Ledger, over HTTP. No method throws: each answers in the board's own words, [Reading] and
 * [WriteResult], so that HTTP stops here and the service never sees a status code.
 *
 * The client is `@Lazy` for the reason given on `LedgerConfiguration.ledgerRestClient`.
 */
@Component
class QuestClient(
    @Lazy private val ledger: RestClient,
) {
    fun quests(): Reading =
        try {
            Reading.Answered(
                ledger
                    .get()
                    .uri("/ledger/quests")
                    .retrieve()
                    .body<List<QuestDto>>()
                    .orEmpty()
                    .map { it.toQuest() },
            )
        } catch (e: Exception) {
            logUnlessClosed(e)
            Reading.Closed
        }

    fun nail(quest: NewQuest): WriteResult =
        write {
            ledger
                .post()
                .uri("/ledger/quests")
                .contentType(MediaType.APPLICATION_JSON)
                .body(NewQuestDto.of(quest))
                .retrieve()
                .toBodilessEntity()
        }

    fun rideOut(id: String): WriteResult =
        write {
            ledger
                .post()
                .uri("/ledger/quests/{id}/riders", id)
                .retrieve()
                .toBodilessEntity()
        }

    /** A status the board has a word for gets that word. Everything else is [WriteResult.CLOSED]. */
    private fun write(call: () -> Unit): WriteResult =
        try {
            call()
            WriteResult.DONE
        } catch (_: HttpClientErrorException.NotFound) {
            WriteResult.GONE
        } catch (e: Exception) {
            logUnlessClosed(e)
            WriteResult.CLOSED
        }

    /**
     * A Ledger that answers 503, or does not answer in time, is closed, and that is no news. Any
     * other failure is a bug here or there. It is closed to the board all the same, since thrown
     * on it would end the board's flow, but the log gets to know.
     */
    private fun logUnlessClosed(e: Exception) {
        val closed = e is HttpServerErrorException.ServiceUnavailable || e is ResourceAccessException
        if (!closed) log.error("The Ledger answered with something the board does not know", e)
    }

    private companion object {
        val log: Logger = LoggerFactory.getLogger(QuestClient::class.java)
    }
}
