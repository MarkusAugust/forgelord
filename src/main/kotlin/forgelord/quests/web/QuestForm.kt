package forgelord.quests.web

import forgelord.quests.NewQuest

/** The page's signals. The form's fields sit under `quest`, so they are `$quest.name` and so on. */
data class QuestSignals(
    val quest: QuestForm = QuestForm(),
)

/** What someone has written in the form: text, every field, exactly as typed. */
data class QuestForm(
    val name: String = "",
    val giver: String = "",
    val text: String = "",
    val reward: String = "",
)

/** One field's verdict: which field, and what is wrong with it. */
data class Fault(
    val field: String,
    val says: String,
)

/** The form, checked: a quest that can go up, or what is wrong with it. */
sealed interface CheckedQuest {
    data class Valid(
        val quest: NewQuest,
    ) : CheckedQuest

    data class Invalid(
        val faults: List<Fault>,
    ) : CheckedQuest
}

private const val NAME_MAX = 40
private const val REWARD_MAX = 1000

/**
 * The rules for a new quest, in one place, on the server.
 *
 * The browser checks nothing. It sends what was typed, and the page shows what the server
 * decided, so there is no second copy of these rules to keep in step. The text is read once
 * here, and what passes comes out as a [NewQuest] that needs no checking again.
 */
fun QuestForm.check(): CheckedQuest {
    val name = name.trim()
    val giver = giver.trim()
    val text = text.trim()
    val reward = reward.trim()
    val marks = reward.toIntOrNull()

    val faults =
        buildList {
            when {
                name.isEmpty() -> add(Fault("name", "A quest needs a name."))
                name.length > NAME_MAX -> add(Fault("name", "No more than $NAME_MAX characters. It has to fit on the post."))
            }
            if (giver.isEmpty()) add(Fault("giver", "Say who is asking. Nobody rides out for a stranger."))
            if (text.isEmpty()) add(Fault("text", "Say what has to be done."))
            when {
                reward.isEmpty() -> add(Fault("reward", "Say what it pays."))
                marks == null -> add(Fault("reward", "A number of marks, in digits."))
                marks < 1 -> add(Fault("reward", "Nobody rides out for nothing."))
                marks > REWARD_MAX -> add(Fault("reward", "More than $REWARD_MAX marks? Nobody in Hookford has that."))
            }
        }

    return if (faults.isEmpty() && marks != null) {
        CheckedQuest.Valid(NewQuest(name = name, giver = giver, text = text, reward = marks))
    } else {
        CheckedQuest.Invalid(faults)
    }
}
