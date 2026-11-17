package forgelord.ledger

import forgelord.page.stylesheet

/** The classes in LedgerPage.css. */
object LedgerPageClasses {
    const val PAGE = "forgelord-ledger-page"
    const val SWITCH = "forgelord-ledger-switch"
}

/** LedgerPage.css, for the page's `<style>`. Read once. */
val ledgerPageCss: String by lazy { stylesheet("forgelord/ledger/LedgerPage.css") }
