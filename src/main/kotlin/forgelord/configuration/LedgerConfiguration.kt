package forgelord.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.core.env.Environment
import org.springframework.web.client.RestClient

@Configuration
class LedgerConfiguration {
    /**
     * The HTTP client for the Ledger, at `forgelord.ledger.url`.
     *
     * `@Lazy` because the Ledger runs in this same app, and its address is this app's own port,
     * which is only known once the server is listening. In a real app the address is fixed
     * configuration, and the bean needs no `@Lazy`.
     */
    @Bean
    @Lazy
    fun ledgerRestClient(
        builder: RestClient.Builder,
        environment: Environment,
    ): RestClient = builder.baseUrl(environment.getRequiredProperty("forgelord.ledger.url")).build()
}
