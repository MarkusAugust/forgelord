package forgelord.quests.client

import forgelord.quests.NewQuest
import forgelord.quests.Quest

/**
 * A quest as the Ledger sends it, in the Ledger's own names.
 *
 * The board has its own [Quest], and this is mapped into it at the border, so a change in the
 * Ledger's JSON stops here and never reaches the views.
 */
data class QuestDto(
    val id: String,
    val title: String,
    val askedBy: String,
    val task: String,
    val marks: Int,
    val riders: Int,
) {
    fun toQuest() = Quest(id = id, name = title, giver = askedBy, text = task, reward = marks, riders = riders)
}

/** A new quest as the Ledger takes it. */
data class NewQuestDto(
    val title: String,
    val askedBy: String,
    val task: String,
    val marks: Int,
) {
    companion object {
        fun of(quest: NewQuest) = NewQuestDto(title = quest.name, askedBy = quest.giver, task = quest.text, marks = quest.reward)
    }
}
