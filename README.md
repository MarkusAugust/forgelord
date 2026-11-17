# Forgelord

A starter for web apps in Kotlin and Spring MVC, with [Streamlord](https://streamlord.sobernetics.no)
and [LO-TEK](https://lo-tek.sobernetics.no). Two lessons: a page that says hello, and a quest board
from [Gallowmark](https://github.com/MarkusAugust/gallowmark) that every open window sees change at
once. Nothing here needs Node or npm.

## How the pages work

The server writes all the HTML. [Datastar](https://data-star.dev), one small script in the browser,
sends what you typed to the server as JSON, and the server answers with new HTML for a part of the
page. Datastar puts it in place by its `id`. The page holds no rules of its own. Streamlord is the
Kotlin side of that: it reads what Datastar sends and writes what it gets back. LO-TEK is the design
system: you call `Lt.button()`, and the button gets LO-TEK's classes.

## Run it

You need Java 21 or newer. Gradle comes with the repository.

```bash
git clone https://github.com/MarkusAugust/forgelord && cd forgelord
./gradlew bootRun
```

Open <http://localhost:8080/hello>. Port 8080 taken? `./gradlew bootRun --args='--server.port=8091'`.

The app restarts by itself when the code is compiled again. In IntelliJ IDEA that is
Build → Build Project (⌘F9). Reload the page to see the change.

Or, without downloading anything, open it in GitHub Codespaces. The app starts by itself, and the
Streamlord extension is installed.

## Lesson 1: Hello

<http://localhost:8080/hello> has one field and one button. Read
[`hello/HelloController.kt`](src/main/kotlin/forgelord/hello/HelloController.kt) from top to bottom.
It is the whole lesson: the page, what happens when you click, and the HTML that comes back.

The exercises are tests that are switched off. Open
[`exercises/HelloExercisesTest.kt`](src/test/kotlin/forgelord/exercises/HelloExercisesTest.kt), delete
the `@Disabled` line above one, and run it from the green arrow beside it. It fails. Change the code
until it passes.

### Exercise 1: Change the greeting

Make the page greet in your own words. There is no test: you are done when the page says it.

<details><summary>Solution</summary>

Change the text in `greetingFor` in `HelloController.kt`, build, and reload the page. Then run the
tests: `HelloControllerTest` checks the old words, so change them there too. That is what a test
is for.

</details>

### Exercise 2: Where are you from?

Add a second field, "Where are you from?". "Ada" from "Hookford" gets "Hello, Ada from Hookford.";
with no place, still "Hello, Ada.".

<details><summary>Solution</summary>

The field is a new signal, so it goes in three places: the class the server reads, the form, and
the greeting.

```kotlin
data class HelloSignals(
    val name: String = "",
    val from: String = "",
)

fun greetingFor(name: String, from: String): String =
    when {
        name.isEmpty() -> "Hello, stranger."
        from.isEmpty() -> "Hello, $name."
        else -> "Hello, $name from $from."
    }
```

In `helloForm`, before the button:

```kotlin
val from = Lt.field(id = "from")
label { lt(from.label); +"Where are you from?" }
input {
    lt(Lt.input(), from.control)
    dataBind("from")
}
```

And in `greet`: `greetingFor(signals.name.trim(), signals.from.trim())`.

</details>

### Exercise 3: Count the greetings

Each greeting ends with its number: "Hello, Bo. Greeting number 2.". Open the page in two windows:
they share the count.

<details><summary>Solution</summary>

The controller is one object for the whole app, so a field on it is shared by every window.

```kotlin
/** One count for the whole app: every window adds to the same number. */
private val greetings = AtomicInteger()
```

In `greet`:

```kotlin
val number = greetings.incrementAndGet()

return response.datastarStream(streamlord, request = request) {
    patchElements(html { greeting("${greetingFor(signals.name.trim(), signals.from.trim())} Greeting number $number.") })
}
```

`AtomicInteger`, not an `Int`, because two windows can click at the same moment. If you skipped
exercise 2, call `greetingFor(signals.name.trim())` instead.

</details>

## Lesson 2: The board

<http://localhost:8080> is the quest board at Hookford. Ride out on a quest, or nail up your own.
Open it in two windows, and both follow.

The board reads its quests from the Ledger, a separate service it calls over HTTP. Here the Ledger
runs in the same app, at <http://localhost:8080/ledger>, where you can open it, wipe it or close it.

The page opens one stream to the server when it loads. Whenever the board changes, the server sends
the whole list down it. "Ride out" and "Nail it up" are commands: they change the Ledger and answer
only the one who clicked. The stream is what shows the change, to every window at once.

Read it in this order:

| Where | What |
| --- | --- |
| `quests/web/ViewController.kt` | The page, the board's stream, and the two commands |
| `quests/web/QuestView.kt` | The page, and the board in each of its four states |
| `quests/QuestService.kt` | The board as one `StateFlow`, read from the Ledger while anyone watches |
| `quests/client/` | The Ledger over HTTP, and its JSON mapped to the board's own types |
| `quests/web/QuestForm.kt` | The rules for a new quest, on the server and nowhere else |
| `components/` | Status message, field, error summary, card list, shell, toast |
| `page/` | The page around everything: LO-TEK, the CSS, the scripts |
| `configuration/` | The Streamlord bean, the Content-Security-Policy, the Ledger's address |
| `ledger/` | The stand-in for a real source service |

The exercises are in
[`exercises/BoardExercisesTest.kt`](src/test/kotlin/forgelord/exercises/BoardExercisesTest.kt).

### Exercise 4: Four states

Keep the board open in one window and `/ledger` in another. Wipe the Ledger, close it, open it.
There is no test: you are done when you have seen the board loading, with quests, empty, and closed.
Nail up a quest while the Ledger is closed, and see what you get.

### Exercise 5: A name already on the post

Nailing up a quest with the name of one already on the post, in any case, shows "A Tenth of the
Wood is already on the post." at the name field, and nothing goes up.

<details><summary>Solution</summary>

The Ledger owns the quests, so the Ledger refuses the name, with `409 Conflict`. Checking on the
board's side would ask the Ledger once more, and two people could still nail up the same name at
the same moment.

In `Ledger.kt`:

```kotlin
@Synchronized
fun has(title: String): Boolean = !closed && entries.any { it.title.equals(title, ignoreCase = true) }
```

In `LedgerController.add`:

```kotlin
if (ledger.has(entry.title)) {
    ResponseEntity.status(HttpStatus.CONFLICT).build()
} else {
    ledger.add(entry)?.let { ResponseEntity.status(HttpStatus.CREATED).body(it) }
        ?: ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
}
```

A new outcome in `WriteResult`, `TAKEN`, and in `QuestService.write`, before the catch for
`RestClientException`:

```kotlin
} catch (_: HttpClientErrorException.Conflict) {
    WriteResult.TAKEN
```

And in `ViewController.nail`, a branch for it:

```kotlin
written == WriteResult.TAKEN -> {
    val name = (checked as CheckedQuest.Valid).quest.name
    patchElements(html { questForm(emptySignals, listOf(Fault("name", "$name is already on the post."))) })
}
```

</details>

### Exercise 6: Turn back

A "Turn back" button beside "Ride out" takes one rider off the quest, never below none. Every open
board shows the new count.

<details><summary>Solution</summary>

A new command, through every layer, the same way "Ride out" goes.

The Ledger, in `Ledger.kt`:

```kotlin
@Synchronized
fun removeRider(id: String): Outcome =
    when {
        closed -> Outcome.CLOSED
        entries.none { it.id == id } -> Outcome.UNKNOWN
        else -> {
            entries = entries.map { if (it.id == id) it.copy(riders = maxOf(0, it.riders - 1)) else it }
            Outcome.DONE
        }
    }
```

and in `LedgerController.kt`, `@DeleteMapping("/ledger/quests/{id}/riders")` answering like
`addRider`, but calling `removeRider`.

The client, in `QuestClient.kt`:

```kotlin
fun turnBack(id: String) {
    ledger.delete().uri("/ledger/quests/{id}/riders", id).retrieve().toBodilessEntity()
}
```

The service: `fun turnBack(id: String): WriteResult = write { client.turnBack(id) }`.

The controller: copy `rideOut` to `@PostMapping("/quests/{id}/turn-back")`, calling
`quests.turnBack(id)`, with "Nobody turned back." in the toast.

And the button, in `questCard` in `QuestView.kt`:

```kotlin
button {
    lt(Lt.button(variant = ButtonVariant.GHOST))
    type = ButtonType.button
    dataOnClick(post("/quests/${quest.id}/turn-back"))
    +"Turn back"
}
```

</details>

### Exercise 7: Break it on purpose

In `questCard`, replace `+quest.name` with `dataText("$quest.name")`. It compiles, because Kotlin
reads `$quest` as its own variable, and the browser never sees a signal. Run the tests:
`AnalysisTest` fails. With the Streamlord plugin in IntelliJ IDEA or VS Code, you see it before you
run anything.

## When you build on it

Day one can skip this part. It is what keeps the app in shape as it grows.

**Rules, and the tests that hold them.** Markup is kotlinx.html, never HTML in strings. A CSS class
is a constant in a `…Classes` object or comes from LO-TEK, never a free string. `unsafe { }` is used
once, for the page's CSS. Seven tests hold the structure:

- `ComponentBoundaryTest`, `PageBoundaryTest`, `QuestsBoundaryTest` and `HelloBoundaryTest` check
  what each package may depend on.
- `ClassesTest` checks that every class constant is in its CSS file, and the other way round.
- `UnsafeTest` checks that `unsafe { }` is where it may be.
- `InstructionsTest` checks that `CLAUDE.md` imports every instruction file.

`AnalysisTest` runs the Streamlord editors' check over every source file.

A new page with its own CSS needs a CSS file, a `…Classes` object beside it, and a line in
`ClassesTest`. A new component's CSS also goes in `ComponentStyles.FILES`.

**Security.** Every page is served with a strict Content-Security-Policy and a new nonce per
request (`configuration/StreamlordConfiguration.kt`). Scripts are files, never inline. Every
command reads the signals, even when it needs none, so a form on another site cannot post to it.

**Your own source service.** To read from a real service instead of the Ledger:

1. Delete `ledger/` and its tests in `src/test/kotlin/forgelord/ledger/`. Take `LedgerPageClasses`
   out of `ClassesTest`, and the steps that close and open the Ledger out of `LiveBoardTest`.
2. Set `forgelord.ledger.url` in `application.properties` to the service's address.
3. Remove `@Lazy` from `LedgerConfiguration` and `QuestClient`. It is only there because the Ledger
   runs in this app, whose port is known late.
4. Change `QuestDto` to the service's JSON.

## For coding agents

The instructions are in `.github/copilot-instructions.md` and `.github/instructions/`, where GitHub
Copilot finds them. `CLAUDE.md` imports the same files for Claude Code.
