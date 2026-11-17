package forgelord.components

import forgelord.Sources
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * `components/` holds what another page could use as it is. It may use `page/`, but not the
 * domain or the configuration, and its CSS says nothing about quests either.
 */
class ComponentBoundaryTest {
    @Test
    fun `components know nothing about the domain or the configuration`() {
        val found = Sources.references(Sources.files("components"), listOf("quests", "ledger", "configuration"))

        assertTrue(found.isEmpty(), "components/ refers to what it must not:\n" + found.joinToString("\n"))
    }

    @Test
    fun `component CSS says nothing about the domain`() {
        val words = listOf("quest", "ledger", "gallowmark", "hookford")
        val found =
            Sources.files("components", "css").flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    if (words.any { line.contains(it, ignoreCase = true) }) "${file.path}:${index + 1}: ${line.trim()}" else null
                }
            }

        assertTrue(found.isEmpty(), "Component CSS names the domain:\n" + found.joinToString("\n"))
    }

    @Test
    fun `every component CSS file is registered in ComponentStyles`() {
        val dir = Sources.root.resolve("components")
        val onDisk = Sources.files("components", "css").map { it.relativeTo(dir).invariantSeparatorsPath }

        assertEquals(onDisk.sorted(), ComponentStyles.FILES.sorted(), "ComponentStyles.FILES and the CSS files differ")
    }
}
