package forgelord.components.shell

import forgelord.page.Content
import kotlinx.html.FlowContent
import kotlinx.html.div
import kotlinx.html.h2
import kotlinx.html.section
import no.lotek.HeadingSize
import no.lotek.Lt
import no.lotek.html.lt

/**
 * A titled section with room for anything: the place to start a new part of a page.
 *
 * It holds the title and the spacing, and nothing else. What goes in it is [content], written
 * where the shell is used.
 */
fun FlowContent.shell(
    title: String,
    content: Content,
) {
    section(ShellClasses.SHELL) {
        h2 { lt(Lt.heading(size = HeadingSize.M)); +title }
        div(ShellClasses.BODY) { content() }
    }
}
