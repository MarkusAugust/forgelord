package forgelord.components.errorsummary

import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.h3
import kotlinx.html.li
import kotlinx.html.ul
import no.lotek.Lt
import no.lotek.html.lt
import no.lotek.html.ltErrorSummary

/** One thing wrong in a form: the field it is about, and what to say. */
data class FieldError(
    val fieldId: String,
    val message: String,
)

/**
 * The list of what is wrong, at the top of a form, each linking to its field.
 *
 * Hidden when there is nothing in it, so it can always be in the form and a morph only fills it.
 */
fun FlowContent.errorSummary(
    id: String,
    errors: List<FieldError>,
) {
    val summary = Lt.errorSummary(count = errors.size, id = id)
    ltErrorSummary {
        lt(summary.host)
        h3 { lt(summary.title); +if (errors.size == 1) "One thing is wrong" else "${errors.size} things are wrong" }
        ul {
            for (error in errors) {
                li { a(href = "#${error.fieldId}") { +error.message } }
            }
        }
    }
}
