package forgelord.quests.web

import forgelord.components.errorsummary.FieldError
import forgelord.components.errorsummary.errorSummary
import forgelord.components.field.textField
import io.github.markusaugust.streamlord.core.json.JsonValue
import io.github.markusaugust.streamlord.html.dataBind
import io.github.markusaugust.streamlord.html.dataOnSubmit
import io.github.markusaugust.streamlord.html.dataSignals
import io.github.markusaugust.streamlord.html.post
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.button
import kotlinx.html.form
import kotlinx.html.id
import no.lotek.Lt
import no.lotek.html.lt

const val QUEST_FORM_ID = "quest-form"

/**
 * The form for a new quest, with the server's verdict on what was sent.
 *
 * The fields are bound to the signals under `quest`, and Datastar sends them all with the post.
 * The answer is this form again, which Datastar morphs into the page by its id: the fields keep
 * what was typed, and the error texts show where the server found something wrong.
 *
 * @param signals The signals the form starts with, from the codec, so their names come from
 *   [QuestSignals] and nowhere else. `ifMissing` keeps a morph from resetting what was typed.
 */
fun FlowContent.questForm(
    signals: JsonValue,
    faults: List<Fault> = emptyList(),
) {
    val says = { field: String -> faults.firstOrNull { it.field == field }?.says }

    form {
        id = QUEST_FORM_ID
        attributes["novalidate"] = ""
        dataSignals(signals, ifMissing = true)
        dataOnSubmit(post("/quests")) { prevent = true }

        errorSummary("quest-errors", faults.map { FieldError(fieldId(it.field), it.says) })

        textField(fieldId("name"), "Name", says("name")) { dataBind("quest.name") }
        textField(fieldId("giver"), "Who is asking", says("giver")) { dataBind("quest.giver") }
        textField(fieldId("text"), "What has to be done", says("text"), multiline = true) { dataBind("quest.text") }
        textField(fieldId("reward"), "Reward", says("reward"), help = "In marks.") {
            // Text, not number: the signal stays the text that was typed, and the server decides
            // whether it is a number.
            attributes["inputmode"] = "numeric"
            dataBind("quest.reward")
        }

        button(classes = QuestPageClasses.SUBMIT) {
            lt(Lt.button())
            type = ButtonType.submit
            +"Nail it up"
        }
    }
}

private fun fieldId(field: String) = "new-$field"
