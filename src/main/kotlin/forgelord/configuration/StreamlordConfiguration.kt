package forgelord.configuration

import io.github.markusaugust.streamlord.core.application.Streamlord
import io.github.markusaugust.streamlord.json.jackson.JacksonSignalsCodec
import io.github.markusaugust.streamlord.spring.CspNonceFilter
import io.github.markusaugust.streamlord.spring.StreamlordExceptionHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import tools.jackson.databind.ObjectMapper

/**
 * Streamlord configures nothing by itself. Its bean is declared here, and
 * [StreamlordExceptionHandler] turns signals that cannot be read into 400, 413 and 415.
 */
@Configuration
@Import(StreamlordExceptionHandler::class)
class StreamlordConfiguration {
    /**
     * Signals are read and written with the Jackson Boot already configured. `compress` gzips a
     * stream when the browser takes it, which pays off here: the board sends the whole list again
     * on every change.
     */
    @Bean
    fun streamlord(mapper: ObjectMapper): Streamlord = Streamlord(codec = JacksonSignalsCodec(mapper), compress = true)

    /**
     * A strict Content-Security-Policy, with a new nonce for every request.
     *
     * Streamlord's starter policy, plus `style-src`: the page's component CSS is an inline
     * `<style>`, which `default-src 'self'` would block, so it carries the nonce too. Datastar
     * reads the nonce from `<html data-nonce>` and needs no `unsafe-eval`.
     */
    @Bean
    fun cspNonceFilter(): CspNonceFilter =
        CspNonceFilter(
            policy = { nonce ->
                "default-src 'self'; script-src 'self' 'nonce-$nonce'; style-src 'self' 'nonce-$nonce'; " +
                    "object-src 'none'; base-uri 'self'; require-trusted-types-for 'script'; trusted-types datastar"
            },
        )
}
