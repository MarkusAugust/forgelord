package forgelord

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Rendered markup, parsed, so a test can ask for an element instead of searching a string.
 *
 * A fragment that only makes sense inside a table, such as a row, has to be wrapped in
 * `table { }` first, or the parser drops it.
 */
fun dom(html: String): Document = Jsoup.parseBodyFragment(html)
