package forgelord

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A signal is named by the property the server reads it into, with the functions in
 * `page/Signals.kt`. A name written as a string is checked by nobody, so there is none.
 */
class SignalNamesTest {
    @Test
    fun `no signal is named by a string`() {
        val found =
            Sources.files("").flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val text = line.trim()
                    val comment = text.startsWith("*") || text.startsWith("/") // KDoc and // lines show the string form.
                    if (!comment && Regex("""\b(dataBind|signal)\(\s*"""").containsMatchIn(text)) "${file.relativeTo(Sources.root).path}:${index + 1}" else null
                }
            }

        assertEquals(emptyList(), found, "Name the signal by its property, as in dataBind(HelloSignals::name)")
    }
}
