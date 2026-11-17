package forgelord.configuration

import jakarta.annotation.PreDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * The scope for work that outlives a request, such as the board asking the Ledger.
 *
 * It is made here and handed to whoever needs it, so a test can hand in its own. What is handed a
 * scope does not end it: this class does, when the app stops.
 */
@Configuration
class CoroutineConfiguration {
    // Dispatchers.IO, since the work is blocking calls to another service. SupervisorJob, so one
    // failure does not end the rest.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Bean
    fun applicationScope(): CoroutineScope = scope

    @PreDestroy
    fun close() = scope.cancel()
}
