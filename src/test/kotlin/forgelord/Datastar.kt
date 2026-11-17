package forgelord

import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.assertj.MockMvcTester
import org.springframework.test.web.servlet.assertj.MvcTestResult
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
