package forgelord.configuration

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.web.servlet.client.RestTestClient
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

/**
 * The policy as a browser gets it, from a running server. The other tests call the controllers
 * directly and never pass the filter, so only this one would notice it gone.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
class ContentSecurityPolicyTest {
    @Autowired
    private lateinit var client: RestTestClient

    @Test
    fun `every page comes with the policy, and carries the policy's nonce`() {
        for (path in listOf("/", "/hello", "/notice", "/together", "/ledger")) {
            val (policy, html) = fetch(path)
            val nonces = Regex("'nonce-([^']+)'").findAll(policy).map { it.groupValues[1] }.toList()

            assertContains(policy, "default-src 'self'", message = path)
            assertFalse("unsafe" in policy, "$path allows something unsafe: $policy")
            // One nonce, for scripts and for styles, and the page has it in both places.
            assertEquals(2, nonces.size, path)
            assertEquals(1, nonces.distinct().size, path)
            assertContains(html, """data-nonce="${nonces.first()}"""", message = path)
            assertContains(html, """<style nonce="${nonces.first()}">""", message = path)
        }
    }

    @Test
    fun `each request gets a nonce of its own`() {
        assertNotEquals(fetch("/").first, fetch("/").first)
    }

    /** The policy header and the page at [path]. */
    private fun fetch(path: String): Pair<String, String> {
        val result = client.get().uri(path).exchange().expectStatus().isOk.returnResult(String::class.java)
        return result.responseHeaders.getFirst("Content-Security-Policy").orEmpty() to result.responseBody.orEmpty()
    }
}
