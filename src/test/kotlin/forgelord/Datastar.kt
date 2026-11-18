package forgelord

import io.github.markusaugust.streamlord.core.domain.CspNonce
import io.github.markusaugust.streamlord.spring.CspNonceFilter
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.assertj.MvcTestResult
import org.springframework.test.web.servlet.client.RestTestClient
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders

/**
 * A request the way Datastar's client sends one, with the signals as JSON in the body.
 *
 * A stream is answered asynchronously, so this dispatches until no async cycle is left, and the
 * response then holds everything the stream sent.
 */
fun MockMvcTester.datastar(
    method: HttpMethod,
    uri: String,
    signals: String = "{}",
): MvcTestResult {
    var result =
        method(method)
            .uri(uri)
            .header("Datastar-Request", "true")
            .header("Accept", "text/event-stream, text/html, application/json")
            .contentType(MediaType.APPLICATION_JSON)
            .content(signals)
            .asyncExchange()
    while (result.unresolvedException == null && result.request.isAsyncStarted) {
        val started = result.mvcResult
        started.getAsyncResult(10_000)
        result = perform(MockMvcRequestBuilders.asyncDispatch(started))
    }
    return result
}

/**
 * A post the way Datastar's client sends one, to a running server. The body is JSON, so the
 * commands' check on the signals lets it through.
 */
fun RestTestClient.datastar(
    path: String,
    signals: String = "{}",
): RestTestClient.ResponseSpec =
    post()
        .uri(path)
        .header("Datastar-Request", "true")
        .contentType(MediaType.APPLICATION_JSON)
        .body(signals)
        .exchange()

/** A request as a controller's `page` gets it: with the nonce `CspNonceFilter` would have put there. */
fun requestWithNonce(): MockHttpServletRequest = MockHttpServletRequest().apply { setAttribute(CspNonceFilter.ATTRIBUTE, CspNonce.generate()) }
