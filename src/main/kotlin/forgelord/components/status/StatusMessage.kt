package forgelord.components.status

import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.p
import kotlinx.html.span
import no.lotek.AlertColor
import no.lotek.Lt
import no.lotek.Size
import no.lotek.html.lt

/** What a status message is telling the reader. */
enum class Status {
    /** Something is on its way. */
    LOADING,

    /** It came, and there is nothing in it. */
    EMPTY,

    /** It did not come. */
    ERROR,
}

/**
 * A short message in place of data that is not there: loading, empty or failed.
 *
 * An error is announced at once (`role="alert"`); the others wait their turn (`role="status"`).
 */
fun FlowContent.statusMessage(
    status: Status,
    text: String,
) {
    div {
        lt(Lt.alert(color = if (status == Status.ERROR) AlertColor.DANGER else AlertColor.INFO))
        attributes["role"] = if (status == Status.ERROR) "alert" else "status"
        if (status == Status.LOADING) {
            span { lt(Lt.spinner(size = Size.SMALL)) }
        }
        p { +text }
    }
}
