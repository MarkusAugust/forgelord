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

/** A quest that has passed the rules and can go up. */
data class NewQuest(
    val name: String,
    val giver: String,
    val text: String,
    val reward: Int,
)
