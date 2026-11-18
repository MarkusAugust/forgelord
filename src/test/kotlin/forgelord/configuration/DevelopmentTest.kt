package forgelord.configuration

import forgelord.page.DevelopmentScript
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.client.RestTestClient
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * The tests run without Spring Boot DevTools, as the jar does. So the running server here shows
 * what production gets: no reload stream and no reload script.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureRestTestClient
class DevelopmentTest {
    @Autowired
    private lateinit var client: RestTestClient

    @Test
    fun `without DevTools there is no reload stream`() {
        client.get().uri("/development/reload").exchange().expectStatus().isNotFound
    }

    @Test
    fun `without DevTools no page loads the reload script`() {
        val html = client.get().uri("/hello").exchange().expectStatus().isOk.returnResult(String::class.java).responseBody.orEmpty()

        assertNull(DevelopmentScript.src)
        assertFalse("development-reload" in html)
    }

    @Test
    fun `the stream says which start answered, and another start says something else`() {
        val first = said(DevelopmentReload())
        val second = said(DevelopmentReload())

        assertContains(first, "retry:300")
        assertContains(first, "data:")
        assertNotEquals(first, second)
    }

    /** What a new connection to [reload] is sent. */
    private fun said(reload: DevelopmentReload): String =
        MockMvcTester.of(reload).get().uri("/development/reload").asyncExchange().response.contentAsString
}
