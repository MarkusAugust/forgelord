package forgelord.quests.web

import forgelord.page.stylesheet

/** The classes in QuestPage.css. */
object QuestPageClasses {
    const val PAGE = "forgelord-quest-page"
    const val HEADER = "forgelord-quest-page-header"
    const val CARD_FOOTER = "forgelord-quest-card-footer"
}

/** QuestPage.css, for the page's `<style>`. Read once. */
val questPageCss: String by lazy { stylesheet("forgelord/quests/web/QuestPage.css") }
