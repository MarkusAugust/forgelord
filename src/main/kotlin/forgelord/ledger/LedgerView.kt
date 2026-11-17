package forgelord.ledger

import forgelord.components.ComponentStyles
import forgelord.components.shell.shell
import forgelord.page.page
import io.github.markusaugust.streamlord.html.dataOnClick
import io.github.markusaugust.streamlord.html.post
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.main
import kotlinx.html.p
import no.lotek.ButtonVariant
import no.lotek.Lt
import no.lotek.ParagraphVariant
import no.lotek.html.lt

const val LEDGER_STATE_ID = "ledger-state"

/** The switch: open the Ledger, wipe it, or close it, and watch the board answer. */
fun ledgerPage(
    nonce: String,
    state: String,
): String =
    page("The Ledger", nonce, ComponentStyles.css + "\n" + ledgerPageCss) {
        main(LedgerPageClasses.PAGE) {
            shell("The Ledger") {
                p {
                    lt(Lt.paragraph(variant = ParagraphVariant.LEAD))
                    +"The service the board reads its quests from. Keep the "
                    a(href = "/") { lt(Lt.link()); +"board" }
                    +" open in another window, and switch the Ledger here."
                }
                div {
                    id = LEDGER_STATE_ID
                    ledgerState(state)
                }
                div(LedgerPageClasses.SWITCH) {
                    switchButton("open", "Open it")
                    switchButton("wipe", "Wipe it")
                    switchButton("close", "Close it")
                }
            }
        }
    }

fun FlowContent.ledgerState(state: String) {
    p { lt(Lt.paragraph()); +state }
}

private fun FlowContent.switchButton(
    action: String,
    label: String,
) {
    button {
        lt(Lt.button(variant = ButtonVariant.SECONDARY))
        type = ButtonType.button
        dataOnClick(post("/ledger/$action"))
        +label
    }
}
