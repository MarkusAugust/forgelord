package forgelord.quests.web

import forgelord.components.errorsummary.FieldError
import forgelord.components.errorsummary.errorSummary
import forgelord.components.field.textField
import forgelord.page.dataBind
import io.github.markusaugust.streamlord.core.json.JsonValue
import io.github.markusaugust.streamlord.html.dataOnSubmit
import io.github.markusaugust.streamlord.html.dataSignals
import io.github.markusaugust.streamlord.html.post
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.HTMLTag
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
    form {
        id = QUEST_FORM_ID
        attributes["novalidate"] = ""
        dataSignals(signals, ifMissing = true)
        dataOnSubmit(post("/quests")) { prevent = true }

        errorSummary("quest-errors", faults.map { FieldError(fieldId(it.field), it.says) })

        questField(QuestForm::name, "Name", faults)
        questField(QuestForm::giver, "Who is asking", faults)
        questField(QuestForm::text, "What has to be done", faults, multiline = true)
        questField(QuestForm::reward, "Reward", faults, help = "In marks.") {
            // Text, not number: the signal stays the text that was typed, and the server decides
            // whether it is a number.
            attributes["inputmode"] = "numeric"
        }

        button {
            lt(Lt.button())
            type = ButtonType.submit
            +"Nail it up"
        }
    }
}

/**
 * One field of the form. Its id, its fault and its signal all come from the one property, so
 * they cannot name different fields.
 */
private fun FlowContent.questField(
    field: Field,
    label: String,
    faults: List<Fault>,
    help: String? = null,
    multiline: Boolean = false,
    control: HTMLTag.() -> Unit = {},
) {
    textField(fieldId(field), label, faults.firstOrNull { it.field == field }?.says, help = help, multiline = multiline) {
        // The signal's path, as QuestSignals nests it: quest.name.
        dataBind(QuestSignals::quest, field)
        control()
    }
}

private fun fieldId(field: Field) = "new-${field.name}"
