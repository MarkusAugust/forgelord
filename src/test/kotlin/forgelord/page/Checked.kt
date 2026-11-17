package forgelord.page

import io.github.markusaugust.streamlord.core.domain.ElementsGuard
import io.github.markusaugust.streamlord.core.domain.CspNonce
import no.lotek.assertLotek

/**
 * [content] as HTML, checked.
 *
 * `ElementsGuard` refuses a Datastar expression Kotlin has eaten, such as `data-text=""` left by
 * the `$` trap. `assertLotek` checks every class, element and `data-*` value against LO-TEK, and,
 * on a whole page around the content, that every label and error text points to a field.
 */
fun checked(content: Content): String {
    val markup = fragment(content)
    ElementsGuard.check(markup)
    assertLotek(page("Test", CspNonce.generate(), "", content))
    return markup
}
