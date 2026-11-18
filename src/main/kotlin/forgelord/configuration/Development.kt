package forgelord.configuration

import forgelord.page.DevelopmentScript
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.web.server.ConfigurableWebServerFactory
import org.springframework.boot.web.server.Shutdown
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import java.util.concurrent.CopyOnWriteArraySet

/** Spring Boot DevTools: on the classpath under `bootRun` and in the IDE, never in the jar. */
private const val DEVTOOLS = "org.springframework.boot.devtools.restart.Restarter"

/**
 * In development, an open page reloads itself when the app has started again.
 *
 * DevTools restarts the app when the code is compiled. This tells the browser: every page holds
 * a stream to `/development/reload`, which says once which start of the app answered. After a
 * restart the page connects again, hears another start, and reloads. `development-reload.js` is
 * the browser's half.
 */
@RestController
@ConditionalOnClass(name = [DEVTOOLS])
class DevelopmentReload {
    /** This start of the app. A restart makes this object again, and with it a new value. */
    private val start = UUID.randomUUID().toString()

    private val open = CopyOnWriteArraySet<SseEmitter>()

    @PostConstruct
    fun addTheScript() {
        DevelopmentScript.src = "/development-reload.js"
    }

    @GetMapping("/development/reload")
    fun reload(): SseEmitter =
        SseEmitter(0L).also { emitter ->
            open += emitter
            emitter.onCompletion { open -= emitter }
            emitter.onError { open -= emitter }
            // reconnectTime: how long the browser waits before it asks again. Its own default is
            // seconds, and the app is back sooner than that.
            emitter.send(SseEmitter.event().reconnectTime(RECONNECT_MILLIS).data(start))
        }

    /** Ends the streams before the server stops, or each one is a warning in the log. */
    @EventListener(ContextClosedEvent::class)
    fun close() = open.forEach { it.complete() }

    private companion object {
        const val RECONNECT_MILLIS = 300L
    }
}

@Configuration
@ConditionalOnClass(name = [DEVTOOLS])
class DevelopmentConfiguration {
    /**
     * In development the server stops at once. A graceful shutdown waits for the open streams,
     * and with the board open that made every restart take four seconds. The price is a warning
     * in the log for each of the board's streams the restart cuts off.
     */
    @Bean
    fun stopAtOnce() = WebServerFactoryCustomizer<ConfigurableWebServerFactory> { it.setShutdown(Shutdown.IMMEDIATE) }
}
