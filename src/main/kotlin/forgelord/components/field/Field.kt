package forgelord.components.field

import kotlinx.html.FlowContent
import kotlinx.html.HTMLTag
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.textArea
import no.lotek.Lt
import no.lotek.RequiredMarker
import no.lotek.html.lt

/**
 * A labelled text field with its help text and error text, wired by `Lt.field`.
 *
 * The error text is always there, hidden until [error] has something to say, so a morph only
 * changes its text and the field's state.
 *
 * @param id The control's id. Make it from a key in the data, never a counter, so it is the same
 *   on every render.
 * @param multiline A `<textarea>` instead of an `<input>`.
 * @param control What the caller adds to the control, such as a Datastar binding.
 */
fun FlowContent.textField(
    id: String,
    label: String,
    error: String?,
    help: String? = null,
    required: Boolean = true,
    multiline: Boolean = false,
    control: HTMLTag.() -> Unit = {},
) {
    val field =
        Lt.field(
            id = id,
            required = if (required) RequiredMarker.SYMBOL else null,
            help = help != null,
            error = true,
            invalid = error != null,
        )
    label { lt(field.label); +label }
    if (multiline) {
        textArea { lt(Lt.textarea(state = field.state), field.control); control() }
    } else {
        input { lt(Lt.input(state = field.state), field.control); control() }
    }
    if (help != null) {
        p { lt(Lt.helpText(), field.help); +help }
    }
    p { lt(Lt.errorText(), field.error); +error.orEmpty() }
}
