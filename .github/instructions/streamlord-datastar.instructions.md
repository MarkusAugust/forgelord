---
name: "Streamlord and Datastar"
description: "Use when: writing a controller, a stream, a command, signals, or any Datastar attribute. Keywords: datastarStream, sendLatest, patchElements, patchSignals, executeScript, readSignals, codec, dataBind, dataOnClick, post, CSP, nonce."
applyTo: "**/src/{main,test}/kotlin/**/{web,configuration,ledger}/**/*.kt, **/src/main/kotlin/**/*Service.kt"
---
# Streamlord and Datastar

The browser runs Datastar; the server speaks to it through Streamlord. The server owns the state and the markup. The browser holds no rules of its own.

## Commands go in, the view comes out

- A page that shows live state opens **one stream** when it loads (`dataInit(get("/board") { requestCancellation = CLEANUP })`) and keeps it open. The stream renders the state with `sendLatest(state, minInterval, resumeFrom)` and patches it `INNER` into the element that opened it. Never `REPLACE` that element: the stream closes when its element leaves the page, and nothing opens it again.
- The state is one `StateFlow` in the service, shared by every stream. Never call the source from inside a stream's render.
- A **command** changes the state and answers for the one who sent it: `204` when it went through, nothing more. The change reaches every page, the sender's too, down the stream.
- A command that fails answers the sender with a toast (`executeScript(showDangerToast(…))`). Datastar runs nothing from an error status, so the answer is a `200` stream.
- A form's verdict is the exception: it answers with the form again, morphed by id, with the faults in the markup. Say so in a comment where you do it.
- A failure of the source is a state (`QuestResult.Unavailable`), never an exception in the stream. A stream that throws ends, and Datastar does not reconnect after an error or a clean end.

## Signals

- Read and write signals with the codec, from one data class: `request.readSignalsOr(QuestSignals(), streamlord)`, `dataSignals(codec.toJsonValue(QuestSignals()), ifMissing = true)`, and `patchSignals(codec.encode(QuestSignals()))` to reset. The names are written once, in the class.
- Name a signal by its property, never by a string: `dataBind(HelloSignals::name)`, `dataBind(QuestSignals::quest, QuestForm::name)` for `quest.name`, and `signal(HelloSignals::name)` in an expression. They are in `page/Signals.kt`, and a name the class does not have does not compile. Where a form has several fields, pass the property on, as `questField(QuestForm::name, …)` does, so the id, the fault and the signal cannot name different fields. Never write `"$quest.name"`: Kotlin takes `$quest` for its own variable, and the browser never sees a signal. `AnalysisTest` and the editors catch it.
- Read the signals in every command, even when it needs none: reading them refuses a body that is not JSON, which is what stops a form on another site from posting to it.

## Controllers

- Take `Streamlord` through the constructor, never as a handler parameter.
- A long-lived stream sets `timeout = Duration.INFINITE` and keeps the heartbeat on.
- `@Import(StreamlordExceptionHandler::class)` is in `configuration/`; do not register it again.

## CSP

- `CspNonceFilter` sets a strict policy with a new nonce for each request. The page puts the nonce on `<html>` with `dataNonce`, so Datastar compiles expressions without `unsafe-eval`, and on the `<style>`.
- Scripts are files from this origin. No inline scripts and no inline `on*` attributes.

## Further reading

- Streamlord: <https://streamlord-docs.netlify.app/>, especially Live views, Spring WebMVC, Signals, Testing and Security.
- LO-TEK in Kotlin: <https://lo-tek.sobernetics.no/agent/kotlin.md>, and with Datastar: <https://lo-tek.sobernetics.no/agent/datastar.md>.
