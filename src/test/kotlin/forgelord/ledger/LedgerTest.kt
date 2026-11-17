package forgelord.ledger

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LedgerTest {
    private val ledger = Ledger()

    @Test
    fun `wiped, it is open with nothing in it`() {
        ledger.wipe()

        assertEquals(emptyList(), ledger.entries())
    }

    @Test
    fun `closed, it answers nothing and takes nothing`() {
        ledger.close()

        assertNull(ledger.entries())
        assertNull(ledger.add(NewLedgerEntry("Cut Ropes", "Ulf", "Six ropes.", 45)))
        assertEquals(Outcome.CLOSED, ledger.addRider("rats-in-the-greywood"))
    }

    @Test
    fun `opened after a wipe, the first quests come back`() {
        ledger.wipe()
        ledger.open()

        assertEquals(3, ledger.entries()?.size)
    }

    @Test
    fun `a new entry goes on top, with an id from its title`() {
        val added = ledger.add(NewLedgerEntry("Cut Ropes", "Ulf", "Six ropes.", 45))

        assertEquals("cut-ropes", added?.id)
        assertEquals("cut-ropes", ledger.entries()?.first()?.id)
    }

    @Test
    fun `a rider for a quest that is not there is unknown`() {
        assertEquals(Outcome.UNKNOWN, ledger.addRider("no-such-quest"))
        assertTrue(ledger.entries().orEmpty().all { it.riders == 0 })
    }
}
