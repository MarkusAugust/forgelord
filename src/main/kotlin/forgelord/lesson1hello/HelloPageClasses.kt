package forgelord.lesson1hello

import forgelord.page.stylesheet

/** The classes in HelloPage.css. A class you write there gets a constant here. */
object HelloPageClasses

/** HelloPage.css, for the page's `<style>`. Read once. */
val helloPageCss: String by lazy { stylesheet("forgelord/lesson1hello/HelloPage.css") }
