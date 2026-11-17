package forgelord.quests.client

import forgelord.quests.NewQuest
import forgelord.quests.Quest
import org.springframework.context.annotation.Lazy
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 * The Ledger, over HTTP. Every method throws a `RestClientException` when the Ledger answers
 * with an error or does not answer at all; the service decides what that means for the board.
 *
 * The client is `@Lazy` for the reason given on `LedgerConfiguration.ledgerRestClient`.
 */
@Component
class QuestClient(
    @Lazy private val ledger: RestClient,
) {
    fun quests(): List<Quest> =
        ledger
            .get()
            .uri("/ledger/quests")
            .retrieve()
            .body<List<QuestDto>>()
            .orEmpty()
            .map { it.toQuest() }

    fun nail(quest: NewQuest) {
        ledger
            .post()
            .uri("/ledger/quests")
            .contentType(MediaType.APPLICATION_JSON)
            .body(NewQuestDto.of(quest))
            .retrieve()
            .toBodilessEntity()
    }

    fun rideOut(id: String) {
        ledger
            .post()
            .uri("/ledger/quests/{id}/riders", id)
            .retrieve()
            .toBodilessEntity()
    }
}
