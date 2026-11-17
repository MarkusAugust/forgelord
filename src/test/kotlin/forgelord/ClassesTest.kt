package forgelord

import forgelord.components.cardlist.CardListClasses
import forgelord.components.shell.ShellClasses
import forgelord.ledger.LedgerPageClasses
import forgelord.quests.web.QuestPageClasses
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Every class constant is in its CSS file, and every class in the CSS file has a constant. A
 * class written as a free string anywhere else would slip past both, which is why there are none.
 */
class ClassesTest {
    /** Each object of class constants, and the CSS file beside it, relative to forgelord/. */
    private val pairs =
        mapOf(
            CardListClasses to "components/cardlist/CardList.css",
            ShellClasses to "components/shell/Shell.css",
            QuestPageClasses to "quests/web/QuestPage.css",
            LedgerPageClasses to "ledger/LedgerPage.css",
        )

    @Test
    fun `the constants and the CSS name the same classes`() {
        for ((classes, css) in pairs) {
            val constants = constants(classes)
            val inCss = classesIn(Sources.root.resolve(css).readText())

            assertEquals(inCss, constants, "${classes::class.simpleName} and $css differ")
        }
    }

    @Test
    fun `every Classes object is checked here`() {
        val objects = Sources.files("").map { it.name }.filter { it.endsWith("Classes.kt") }.toSet()
        val checked = pairs.keys.map { "${it::class.simpleName}.kt" }.toSet()

        assertTrue(objects == checked, "Add these to ClassesTest: ${objects - checked}")
    }

    private fun constants(classes: Any): Set<String> =
        classes::class.java.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && it.type == String::class.java }
            .map { it.get(null) as String }
            .toSet()

    private fun classesIn(css: String): Set<String> {
        val withoutComments = css.replace(Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL), "")
        return Regex("""\.([a-zA-Z][\w-]*)""").findAll(withoutComments).map { it.groupValues[1] }.toSet()
    }
}
