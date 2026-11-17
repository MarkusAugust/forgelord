package forgelord

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Copilot finds the files in .github/instructions/ by itself. Claude finds only what CLAUDE.md
 * imports, so a new instruction file that is not imported there would reach one agent and not the
 * other.
 */
class InstructionsTest {
    @Test
    fun `CLAUDE md imports every instruction file`() {
        val onDisk =
            File(".github/instructions").listFiles { file -> file.name.endsWith(".instructions.md") }.orEmpty()
                .map { ".github/instructions/${it.name}" }
                .sorted()
        val imported =
            File("CLAUDE.md").readLines()
                .filter { it.startsWith("@.github/instructions/") }
                .map { it.removePrefix("@") }
                .sorted()

        assertEquals(onDisk, imported, "CLAUDE.md and .github/instructions/ differ")
    }
}
