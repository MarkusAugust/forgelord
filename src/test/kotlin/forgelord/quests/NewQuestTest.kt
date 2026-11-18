package forgelord.quests

import forgelord.Sources
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * A [NewQuest] says it has passed the rules, and that is only true while the rules are the one
 * place that makes it. A second caller that builds its own would send the Ledger a quest nobody
 * has checked.
 */
class NewQuestTest {
    @Test
    fun `a new quest is made only where the rules are`() {
        val found =
            Sources.files("").flatMap { file ->
                file.readLines().mapIndexedNotNull { index, line ->
                    val makes = Regex("""\bNewQuest\(""").containsMatchIn(line) && !line.contains("class NewQuest(")
                    if (makes) "${file.relativeTo(Sources.root).invariantSeparatorsPath}:${index + 1}" else null
                }
            }

        assertEquals(listOf("quests/web/QuestForm.kt"), found.map { it.substringBefore(":") }, "NewQuest( found in:\n" + found.joinToString("\n"))
    }
}
