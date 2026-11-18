package forgelord.page

import io.github.markusaugust.streamlord.html.dataNonce
import kotlinx.html.body
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.lang
import kotlinx.html.link
import kotlinx.html.meta
import kotlinx.html.script
import kotlinx.html.stream.createHTML
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import no.lotek.LotekWebJar

/**
 * The order of the CSS layers: LO-TEK first, then the components, then the page. A later layer
 * wins over an earlier one, whatever the specificity, so a component can adjust LO-TEK and the
 * page can adjust a component. CSS outside a layer would beat them all, so there is none.
 */
const val LAYER_ORDER = "@layer lotek, forgelord-components, forgelord-page;"

/** Page.css: what every page has, whatever is on it. Read once. */
private val pageCss: String by lazy { stylesheet("forgelord/page/Page.css") }

/**
 * A script every page loads in development, and none otherwise. `page/` does not know what it is
 * for: `configuration/DevelopmentReload.kt` sets it when the app starts.
 */
object DevelopmentScript {
    @Volatile
    var src: String? = null
}

/**
 * The page around everything: LO-TEK's stylesheet, the CSS given in [css], and the scripts.
 *
 * In this package because it is the same for every page, and knows nothing about what is on it.
 * In an app of embeddable widgets, the same thing usually lives in `widget/`.
 *
 * @param nonce This request's CSP nonce. Datastar reads it from `<html>`, and the `<style>`
 *   carries it, so neither needs the policy to allow inline code.
 * @param css The CSS the page needs beyond LO-TEK, each file already in its layer.
 */
fun page(
    title: String,
    nonce: String,
    css: String,
    content: Content,
): String {
    // The one place unsafe is allowed. CSS is written as it stands, and a `</` in it could end
    // the <style> and start something else, so it is refused.
    require("</" !in css) { "The page's CSS contains </, which would end the <style> element" }

    return "<!DOCTYPE html>\n" +
        createHTML().html {
            lang = "en"
            dataNonce(nonce)
            head {
                meta(charset = "utf-8")
                meta(name = "viewport", content = "width=device-width, initial-scale=1")
                title(title)
                link(rel = "stylesheet", href = LotekWebJar.CSS)
                style {
                    attributes["nonce"] = nonce
                    unsafe { +"$LAYER_ORDER\n$pageCss\n$css" }
                }
            }
            body {
                content()
                // LO-TEK's web components are registered before Datastar runs: module scripts run
                // in the order they stand. Both are files, not inline scripts; see forgelord.js.
                script(type = "module", src = "/forgelord.js") {}
                script(type = "module", src = "/datastar.js") {}
                DevelopmentScript.src?.let { script(type = "module", src = it) {} }
            }
        }
}
