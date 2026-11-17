package forgelord

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Forgelord

fun main(args: Array<String>) {
    runApplication<Forgelord>(*args)
}
