package forgelord.quests.web

import forgelord.quests.NewQuest
import forgelord.quests.PassedTheRules
import kotlin.test.Test
import kotlin.test.assertEquals

/** The rules are a function from what was typed to a quest or its faults, so they need no server. */
@OptIn(PassedTheRules::class)
class QuestFormTest {
    private val good = QuestForm(name = " Cut Ropes ", giver = "Ulf Eightrope", text = "Bring six cut ropes.", reward = "45")

    private fun faultsOf(form: QuestForm) = (form.check() as CheckedQuest.Invalid).faults

    @Test
    fun `an empty form is wrong in every field`() {
        assertEquals(listOf(QuestForm::name, QuestForm::giver, QuestForm::text, QuestForm::reward), faultsOf(QuestForm()).map { it.field })
    }

    @Test
    fun `a whole quest comes out trimmed, with its reward as a number`() {
        assertEquals(CheckedQuest.Valid(NewQuest.of("Cut Ropes", "Ulf Eightrope", "Bring six cut ropes.", 45)), good.check())
    }

    @Test
    fun `the reward is a number of marks, and at least one`() {
        assertEquals("A number of marks, in digits.", faultsOf(good.copy(reward = "a lot")).single().says)
        assertEquals("Nobody rides out for nothing.", faultsOf(good.copy(reward = "0")).single().says)
        assertEquals(QuestForm::reward, faultsOf(good.copy(reward = "5000")).single().field)
    }

    @Test
    fun `a name has to fit on the post`() {
        assertEquals(QuestForm::name, faultsOf(good.copy(name = "A".repeat(41))).single().field)
    }
}
