package forgelord.lesson3together

import forgelord.page.stylesheet

/** The classes in TogetherPage.css. A class you write there gets a constant here. */
object TogetherPageClasses {
    /** The count on a card. */
    const val RIDERS = "forgelord-together-riders"
}

/** TogetherPage.css, for the page's `<style>`. Read once. */
val togetherPageCss: String by lazy { stylesheet("forgelord/lesson3together/TogetherPage.css") }
