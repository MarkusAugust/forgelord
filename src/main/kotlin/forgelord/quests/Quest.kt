package forgelord.quests

/** A quest on the board at Hookford: who wants it done, what they want, and what it pays. */
data class Quest(
    val id: String,
    val name: String,
    val giver: String,
    val text: String,
    val reward: Int,
    val riders: Int,
)

/**
 * Marks the one way to make a [NewQuest]. Code that calls it has to say
 * `@OptIn(PassedTheRules::class)`, or it does not compile. `NewQuestTest` keeps that line to the
 * file with the rules.
 */
@RequiresOptIn("A NewQuest has passed the rules for a new quest. Only QuestForm.check() makes one.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class PassedTheRules

/**
 * A quest that has passed the rules and can go up.
 *
 * The constructor is private, and so is `copy`, so the only way to one is [of], and [of] is
 * marked: nobody makes a quest that skipped the rules without saying so in the code.
 */
@ConsistentCopyVisibility
data class NewQuest private constructor(
    val name: String,
    val giver: String,
    val text: String,
    val reward: Int,
) {
    companion object {
        @PassedTheRules
        fun of(
            name: String,
            giver: String,
            text: String,
            reward: Int,
        ) = NewQuest(name, giver, text, reward)
    }
}
