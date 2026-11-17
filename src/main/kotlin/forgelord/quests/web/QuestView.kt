package forgelord.quests.web

import forgelord.components.ComponentStyles
import forgelord.components.cardlist.cardList
import forgelord.components.shell.shell
import forgelord.components.status.Status
import forgelord.components.status.statusMessage
import forgelord.components.toast.toastRegion
import forgelord.page.page
import forgelord.quests.Quest
import forgelord.quests.QuestResult
import io.github.markusaugust.streamlord.core.json.JsonValue
import io.github.markusaugust.streamlord.html.FetchOptions
import io.github.markusaugust.streamlord.html.dataInit
import io.github.markusaugust.streamlord.html.dataOnClick
import io.github.markusaugust.streamlord.html.get
import io.github.markusaugust.streamlord.html.post
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h3
import kotlinx.html.header
import kotlinx.html.id
import kotlinx.html.main
import kotlinx.html.p
import kotlinx.html.span
import no.lotek.BadgeColor
import no.lotek.ButtonVariant
import no.lotek.HeadingSize
import no.lotek.Lt
import no.lotek.ParagraphVariant
import no.lotek.Size
import no.lotek.html.lt

const val BOARD_ID = "board"
const val TOASTS_ID = "toasts"

/**
 * The whole page, as the browser gets it on its first visit and on every reload.
 *
 * The board starts as [QuestResult.Loading], so the page never waits for the Ledger. The board's
 * stream opens as the page loads and replaces it.
 */
fun questPage(
    nonce: String,
    signals: JsonValue,
): String =
    page("The board at Hookford", nonce, ComponentStyles.css + "\n" + questPageCss) {
        main(QuestPageClasses.PAGE) {
            header(QuestPageClasses.HEADER) {
                h1 { lt(Lt.heading(size = HeadingSize.XL)); +"The board at Hookford" }
                p {
                    lt(Lt.paragraph(variant = ParagraphVariant.LEAD))
                    +"Quests nailed to the post by the bridge. Ride out on one, or nail up your own."
                }
            }
            shell("On the post") {
                // One stream for the page, held open. Every change to the board, from any page,
                // comes down it. `cleanup` ends it when the element leaves the page, and the
                // retries outlast a deploy or two: Datastar counts them over the stream's life.
                div {
                    id = BOARD_ID
                    dataInit(
                        get("/board") {
                            requestCancellation = FetchOptions.RequestCancellation.CLEANUP
                            retryMaxCount = 1000
                        },
                    )
                    boardView(QuestResult.Loading)
                }
            }
            shell("Nail up a quest") {
                questForm(signals)
            }
            toastRegion(TOASTS_ID)
        }
    }

/** The board in each of its four states. The stream sends this again whenever the state changes. */
fun FlowContent.boardView(result: QuestResult) {
    when (result) {
        QuestResult.Loading -> statusMessage(Status.LOADING, "Reading the Ledger.")
        QuestResult.Empty -> statusMessage(Status.EMPTY, "The post is bare. Nail up the first quest.")
        QuestResult.Unavailable ->
            statusMessage(Status.ERROR, "The Ledger is closed. The board comes back by itself when it opens.")
        is QuestResult.Found -> cardList(result.quests, cardId = { "quest-${it.id}" }) { questCard(it) }
    }
}

private fun FlowContent.questCard(quest: Quest) {
    h3 { lt(Lt.card.title); +quest.name }
    p { lt(Lt.paragraph(size = Size.SMALL)); +"Asked by ${quest.giver}" }
    p { lt(Lt.paragraph()); +quest.text }
    div(QuestPageClasses.CARD_FOOTER) {
        span { lt(Lt.badge(color = BadgeColor.SUCCESS)); +"${quest.reward} marks" }
        p { lt(Lt.paragraph(size = Size.SMALL)); +riders(quest.riders) }
        button {
            lt(Lt.button(variant = ButtonVariant.SECONDARY))
            type = ButtonType.button
            // A command: the server tells the Ledger and answers 204. The new count reaches this
            // page, and every other, down the board's stream.
            dataOnClick(post("/quests/${quest.id}/ride"))
            +"Ride out"
        }
    }
}

private fun riders(count: Int): String =
    when (count) {
        0 -> "Nobody has ridden out yet."
        1 -> "One has ridden out."
        else -> "$count have ridden out."
    }
