package forgelord.page

import io.github.markusaugust.streamlord.core.domain.CspNonce
import kotlinx.html.p
import no.lotek.LotekWebJar
import no.lotek.assertLotek
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PageTest {
    private val nonce = CspNonce.generate()

    @Test
    fun `the page is correct markup`() {
        assertLotek(page("Test", nonce, "", { p { +"Hello" } }))
    }

    @Test
    fun `the nonce is on html for Datastar and on the style`() {
        val html = page("Test", nonce, ".a { color: red; }", {})

        assertContains(html, """data-nonce="$nonce"""")
        assertContains(html, """<style nonce="$nonce">""")
        assertContains(html, LAYER_ORDER)
    }

    @Test
    fun `CSS that could end the style element is refused`() {
        assertFailsWith<IllegalArgumentException> { page("Test", nonce, "</style><script>", {}) }
    }

    @Test
    fun `forgelord js imports the register module of the LO-TEK version in the build`() {
        val script = File("src/main/resources/static/forgelord.js").readText()
        val imported = Regex("""from "([^"]+)"""").find(script)?.groupValues?.get(1)

        assertEquals(LotekWebJar.REGISTER, imported, "Update the path in forgelord.js to the LO-TEK version in build.gradle.kts")
    }
}
