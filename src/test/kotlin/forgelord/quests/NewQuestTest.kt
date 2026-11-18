package forgelord.quests

import forgelord.Sources
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The compiler makes whoever makes a [NewQuest] say `@OptIn(PassedTheRules::class)`. It does not
 * stop a second file from saying it, so this does: the rules are in one file, and that file is
 * the only one that opts in.
 */
class NewQuestTest {
    @Test
    fun `only the file with the rules may make a new quest`() {
        val found =
            Sources.files("")
                .filter { file -> file.readLines().any { "OptIn(PassedTheRules::class)" in it && !it.trim().startsWith("*") } }
                .map { it.relativeTo(Sources.root).invariantSeparatorsPath }

        assertEquals(listOf("quests/web/QuestForm.kt"), found)
    }
}
