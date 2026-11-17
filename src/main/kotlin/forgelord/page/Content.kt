package forgelord.page

import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.stream.createHTML

/** Markup passed around: a piece of a page, written where it is used. */
typealias Content = FlowContent.() -> Unit

/**
 * A piece of a page as HTML, for a patch or a test.
 *
 * kotlinx.html needs a tag to write into, so the content is written into a `div` and the `div`
 * is taken off again.
 */
fun html(content: Content): String =
    createHTML(prettyPrint = false)
        .div { content() }
        .removePrefix("<div>")
        .removeSuffix("</div>")
