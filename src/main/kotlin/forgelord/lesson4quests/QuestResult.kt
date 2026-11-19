package forgelord.lesson4quests

/**
 * What the board shows. One of four, and the view has one rendering for each.
 *
 * A Ledger that does not answer is [Unavailable], a state like the others, not an exception. The
 * stream renders it and stays open, and the board comes back by itself when the Ledger does.
 */
sealed interface QuestResult {
    /** Nothing asked yet. */
    data object Loading : QuestResult

    data class Found(
        val quests: List<Quest>,
    ) : QuestResult

    /** The Ledger answered, with no quests. */
    data object Empty : QuestResult

    /** The Ledger did not answer. */
    data object Unavailable : QuestResult
}

/** What the Ledger answered when the board asked for its quests. */
sealed interface Reading {
    data class Answered(
        val quests: List<Quest>,
    ) : Reading

    /** The Ledger did not answer, or answered with something the board cannot read. */
    data object Closed : Reading
}

/** What became of a write: a ride or a new quest. */
enum class WriteResult {
    DONE,

    /** The Ledger did not answer, or answered with something the board does not know. Nothing changed. */
    CLOSED,

    /** The Ledger no longer has the quest. Nothing changed. */
    GONE,
}
