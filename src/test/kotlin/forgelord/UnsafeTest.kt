package forgelord

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `unsafe { }` writes text into the page without escaping, so it is the one way markup or script
 * from outside could get in. It is allowed once: for the CSS in `page/Page.kt`, which refuses `</`.
 */
class UnsafeTest {
    @Test
    fun `unsafe is used only for the page's CSS`() {
        val found =
            Sources.files("").flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (Regex("""\bunsafe\s*\{""").containsMatchIn(line)) "${file.relativeTo(Sources.root).path}:${index + 1}" else null
                }
            }

        assertEquals(1, found.size, "unsafe { } found in:\n" + found.joinToString("\n"))
        assertEquals(true, found.single().startsWith("page/Page.kt:"), "unsafe { } belongs only in page/Page.kt: $found")
    }
}
