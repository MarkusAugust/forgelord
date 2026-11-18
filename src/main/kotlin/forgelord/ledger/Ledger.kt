package forgelord.ledger

import org.springframework.stereotype.Component

/** A quest as the Ledger writes it down. Its names are the Ledger's own, not the board's. */
data class LedgerEntry(
    val id: String,
    val title: String,
    val askedBy: String,
    val task: String,
    val marks: Int,
    val riders: Int = 0,
)

/** A new entry, as the Ledger takes it. */
data class NewLedgerEntry(
    val title: String,
    val askedBy: String,
    val task: String,
    val marks: Int,
)

/** What the Ledger did with a write. */
enum class Outcome { DONE, CLOSED, UNKNOWN }

/**
 * The Ledger: a stand-in for the service a real app would read its data from.
 *
 * It keeps its entries in memory, and the switch on /ledger opens it, wipes it or closes it, so
 * the board can be seen with quests, with none, and with the Ledger gone. Nothing in `quests/`
 * knows it is here: the board only ever reaches it over HTTP.
 */
@Component
class Ledger {
    private var entries = FIRST_ENTRIES
    private var closed = false

    @Synchronized
    fun entries(): List<LedgerEntry>? = if (closed) null else entries

    @Synchronized
    fun add(entry: NewLedgerEntry): LedgerEntry? {
        if (closed) return null
        val added = LedgerEntry(slug(entry.title), entry.title, entry.askedBy, entry.task, entry.marks)
        entries = listOf(added) + entries
        return added
    }

    @Synchronized
    fun addRider(id: String): Outcome = changeRiders(id) { it + 1 }

    /** One rider fewer, and never fewer than none. */
    @Synchronized
    fun removeRider(id: String): Outcome = changeRiders(id) { maxOf(0, it - 1) }

    private fun changeRiders(
        id: String,
        change: (Int) -> Int,
    ): Outcome =
        when {
            closed -> Outcome.CLOSED
            entries.none { it.id == id } -> Outcome.UNKNOWN
            else -> {
                entries = entries.map { if (it.id == id) it.copy(riders = change(it.riders)) else it }
                Outcome.DONE
            }
        }

    /** Opens the Ledger. If it was wiped, the first quests come back. */
    @Synchronized
    fun open() {
        closed = false
        if (entries.isEmpty()) entries = FIRST_ENTRIES
    }

    /** Leaves the Ledger open, with nothing in it. */
    @Synchronized
    fun wipe() {
        closed = false
        entries = emptyList()
    }

    /** Closes the Ledger: every request answers 503 until it opens again. */
    @Synchronized
    fun close() {
        closed = true
    }

    @Synchronized
    fun describe(): String =
        when {
            closed -> "Closed. Every request gets 503."
            entries.isEmpty() -> "Open, with no quests."
            entries.size == 1 -> "Open, with one quest."
            else -> "Open, with ${entries.size} quests."
        }

    /** An id made from the title, and kept unique. */
    private fun slug(title: String): String {
        val base = title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifEmpty { "quest" }
        val taken = entries.map { it.id }.toSet()
        return generateSequence(1) { it + 1 }.map { if (it == 1) base else "$base-$it" }.first { it !in taken }
    }
}

/** Three quests from Harth: Year 150, so the board is not empty the first time it opens. */
private val FIRST_ENTRIES =
    listOf(
        LedgerEntry(
            id = "rats-in-the-greywood",
            title = "Rats in the Greywood",
            askedBy = "A Sergeant of the Unmourned",
            task = "You want pay? Good. Everyone does. The company is camped two nights at the ford and " +
                "the rats are in the baggage. Not ours. Knotrats, out of the Greywood, west. Kill eight.",
            marks = 15,
        ),
        LedgerEntry(
            id = "a-tenth-of-the-wood",
            title = "A Tenth of the Wood",
            askedBy = "The Bridge-Captain",
            task = "Pardon. I see you have been in the Greywood. The order takes a tenth of what comes " +
                "out of it. Five of grey wood, five of knotrat hide, if you would be so kind. " +
                "I apologise for the inconvenience. It is the custom.",
            marks = 30,
        ),
        LedgerEntry(
            id = "what-the-river-carries",
            title = "What the River Carries",
            askedBy = "A River-Priest of Thurn",
            task = "You ask why the rats chew the ropes. Hask. You ask who breeds them. The Kneeling. " +
                "You ask what you should do. Ulf. Go north to Hangmoot, to the Reeve. Say the river sent you.",
            marks = 10,
        ),
    )
