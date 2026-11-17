package forgelord.ledger

import io.github.markusaugust.streamlord.core.domain.CspNonce
import no.lotek.assertLotek
import kotlin.test.Test

class LedgerViewTest {
    @Test
    fun `the Ledger's page is correct markup`() {
        assertLotek(ledgerPage(CspNonce.generate(), Ledger().describe()))
    }
}
