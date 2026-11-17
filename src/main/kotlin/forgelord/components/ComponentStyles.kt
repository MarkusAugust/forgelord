package forgelord.components

import forgelord.page.stylesheet

/**
 * Every component's CSS file, relative to `components/`. A new CSS file goes here, or
 * ComponentStylesTest fails.
 */
object ComponentStyles {
    val FILES =
        listOf(
            "cardlist/CardList.css",
            "shell/Shell.css",
        )

    /** The files' CSS, for the page's `<style>`. Read once. */
    val css: String by lazy { FILES.joinToString("\n") { stylesheet("forgelord/components/$it") } }
}
