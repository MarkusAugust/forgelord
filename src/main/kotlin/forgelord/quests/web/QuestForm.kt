package forgelord.quests.web

import forgelord.quests.NewQuest
import forgelord.quests.PassedTheRules
import kotlin.reflect.KProperty1

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

/** One of the form's fields, named by its property: `QuestForm::name`. */
typealias Field = KProperty1<QuestForm, String>

/**
 * One field's verdict: which field, and what is wrong with it.
 *
 * The field is the property itself, not its name in a string, so a field that does not exist
 * does not compile, and the names are written once, in [QuestForm].
 */
data class Fault(
    val field: Field,
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
 * here, and what passes comes out as a [NewQuest] that needs no checking again. This is the one
 * place that may make one, which is what the `@OptIn` says.
 */
@OptIn(PassedTheRules::class)
fun QuestForm.check(): CheckedQuest {
    val trimmedName = name.trim()
    val trimmedGiver = giver.trim()
    val trimmedText = text.trim()
    val trimmedReward = reward.trim()
    val marks = trimmedReward.toIntOrNull()

    val faults =
        buildList {
            when {
                trimmedName.isEmpty() -> add(Fault(QuestForm::name, "A quest needs a name."))
                trimmedName.length > NAME_MAX -> add(Fault(QuestForm::name, "No more than $NAME_MAX characters. It has to fit on the post."))
            }
            if (trimmedGiver.isEmpty()) add(Fault(QuestForm::giver, "Say who is asking. Nobody rides out for a stranger."))
            if (trimmedText.isEmpty()) add(Fault(QuestForm::text, "Say what has to be done."))
            when {
                trimmedReward.isEmpty() -> add(Fault(QuestForm::reward, "Say what it pays."))
                marks == null -> add(Fault(QuestForm::reward, "A number of marks, in digits."))
                marks < 1 -> add(Fault(QuestForm::reward, "Nobody rides out for nothing."))
                marks > REWARD_MAX -> add(Fault(QuestForm::reward, "More than $REWARD_MAX marks? Nobody in Hookford has that."))
            }
        }

    return if (faults.isEmpty() && marks != null) {
        CheckedQuest.Valid(NewQuest.of(name = trimmedName, giver = trimmedGiver, text = trimmedText, reward = marks))
    } else {
        CheckedQuest.Invalid(faults)
    }
}
