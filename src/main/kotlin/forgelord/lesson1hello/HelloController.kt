package forgelord.lesson1hello

import forgelord.page.dataBind
import forgelord.page.fragment
import forgelord.page.page
import io.github.markusaugust.streamlord.core.application.Streamlord
import io.github.markusaugust.streamlord.html.dataOnSubmit
import io.github.markusaugust.streamlord.html.post
import io.github.markusaugust.streamlord.spring.cspNonce
import io.github.markusaugust.streamlord.spring.datastarStream
import io.github.markusaugust.streamlord.spring.readSignalsOr
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.main
import kotlinx.html.p
import no.lotek.HeadingSize
import no.lotek.Lt
import no.lotek.ParagraphVariant
import no.lotek.html.lt
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

/**
 * Lesson 1: one page, one field, one round trip.
 *
 * 1. GET /hello sends the whole page.
 * 2. The field is bound to the signal `name`. Datastar keeps it up to date as you type.
 * 3. "Say hello" posts the signals to /hello as JSON: `{"name": "Ada"}`.
 * 4. The server reads them into [HelloSignals], and answers with one element: the greeting.
 * 5. Datastar finds the element on the page with the same id and puts the new one in its place.
 *
 * The page, the command and the view are in this one file, so it can be read from top to bottom. A
 * bigger page splits it into a controller and a view, as the board in lesson4quests/web/ does.
 */
@RestController
class HelloController(
    private val streamlord: Streamlord,
) {
    @GetMapping("/hello", produces = [MediaType.TEXT_HTML_VALUE])
    fun page(request: HttpServletRequest): String =
        page("Hello Gallowmark", request.cspNonce, css = helloPageCss) {
            main {
                h1 { lt(Lt.heading(size = HeadingSize.XL)); +"Hello Gallowmark" }
                helloForm()
                greeting("Write your name, and say hello.")
            }
        }

    @PostMapping("/hello")
    fun greet(
        request: HttpServletRequest,
        response: HttpServletResponse,
    ): StreamingResponseBody {
        val signals = request.readSignalsOr(HelloSignals(), streamlord)

        return response.datastarStream(streamlord, request = request) {
            patchElements(fragment { greeting(greetingFor(signals.name.trim())) })
        }
    }
}

/** The signals the page sends. Each field has the name of a signal on the page. */
data class HelloSignals(
    val name: String = "",
)

fun greetingFor(name: String): String = if (name.isEmpty()) "Hello, stranger." else "Hello, $name."

private fun FlowContent.helloForm() {
    form {
        // Datastar posts the signals instead of the browser posting the form.
        dataOnSubmit(post("/hello")) { prevent = true }

        // A field's parts go in one div. The page puts room between a form's children, and
        // none inside one.
        div {
            val field = Lt.field(id = "name")
            label { lt(field.label); +"Your name" }
            input {
                lt(Lt.input(), field.control)
                dataBind(HelloSignals::name)
            }
        }
        button {
            lt(Lt.button())
            type = ButtonType.submit
            +"Say hello"
        }
    }
}

/** The one element the answer replaces, in step 5. */
private fun FlowContent.greeting(text: String) {
    p {
        id = "greeting"
        lt(Lt.paragraph(variant = ParagraphVariant.LEAD))
        +text
    }
}
