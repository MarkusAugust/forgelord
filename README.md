# Forgelord

A starter for web apps in Kotlin and Spring MVC, with [Streamlord](https://streamlord.sobernetics.no)
and [LO-TEK](https://lo-tek.sobernetics.no). Four lessons, each a step from the one before: a page
that says hello, a list with a button, the same list kept up to date in every open window, and a
quest board from [Gallowmark](https://github.com/MarkusAugust/gallowmark) that reads its quests
from another service. Nothing here needs Node or npm.

## How the pages work

The server writes all the HTML. [Datastar](https://data-star.dev), one small script in the browser,
sends what you typed to the server as JSON, and the server answers with new HTML for a part of the
page. You write no JavaScript: what to show, and what is valid, is decided on the server.
Streamlord is the Kotlin side of that: it reads what Datastar sends and writes what it gets back. LO-TEK is the design
system: you call `Lt.button()`, and the button gets LO-TEK's classes.

The words the code and the lessons use:

- **Signal.** A named value Datastar keeps on the page. `dataBind("name")` ties a field to the
  signal `name`, and Datastar sends every signal as JSON with each request: `{"name": "Ada"}`.
- **Patch.** HTML the server sends for one part of the page. Datastar finds the element on the page
  with the same `id` and puts the new one in its place.
- **Morph.** How Datastar puts it in place: it changes only what differs, so a field you are typing
  in keeps its text and the cursor.
- **Stream.** Every Datastar answer here is a stream of server-sent events (SSE). In lessons 1
  and 2 it carries one patch and closes. From lesson 3, the server keeps it open and writes a
  patch whenever something changes.
- **Command.** A POST that changes something on the server, such as "Ride out".
- **Bean.** An object Spring creates once, when the app starts, and keeps. Each class marked
  `@RestController`, `@Service` or `@Component` becomes one, and so does what each `@Bean` function in
  `configuration/` returns.
- **Injection.** Spring passes each bean the beans its constructor asks for. `HelloController` asks
  for a `Streamlord`, and gets the one `StreamlordConfiguration` made.
- **`application.properties`.** The app's settings, in `src/main/resources/`. Spring reads them
  when the app starts.

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

`greet` reads the signals with `readSignalsOr(HelloSignals(), streamlord)`. It turns the JSON into
a `HelloSignals`. A signal missing from the JSON gets its default in the class, and a request with
no signals at all gets the `HelloSignals()` given in the call. A request whose body is not JSON is
refused with `415 Unsupported Media Type`.

The exercises are tests that are switched off. Open
[`exercises/HelloExercisesTest.kt`](src/test/kotlin/forgelord/exercises/HelloExercisesTest.kt), delete
the `@Disabled` line above one, and run it from the green arrow beside it. It fails. Change the code
until it passes.

The solutions leave out the imports. When a name turns red in IntelliJ IDEA, put the cursor on it
and press Alt+Enter (⌥↩ on a Mac) to import it.

### Exercise 1: Change the greeting

Make the page greet in your own words. There is no test: you are done when the page says it.

<details><summary>Solution</summary>

Change the text in `greetingFor` in `HelloController.kt`, build, and reload the page. Then run the
tests: `HelloControllerTest` checks the old words, so change them there too.

</details>

### Exercise 2: Where are you from?

Add a second field, "Where are you from?". "Ada" from "Hookford" gets "Hello, Ada from Hookford.";
with no place, still "Hello, Ada.". If you changed the words in exercise 1, keep yours: the test
checks only the name and the place.

<details><summary>Solution</summary>

The field is a new signal, so it goes in four places: the class the server reads, the greeting,
the form, and `greet`, which passes the signal on. `when` with nothing after it tries each
condition in turn.

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
/** How many greetings, from every window. */
private val greetings = AtomicInteger()
```

In `greet`:

```kotlin
val number = greetings.incrementAndGet()

return response.datastarStream(streamlord, request = request) {
    patchElements(fragment { greeting("${greetingFor(signals.name.trim(), signals.from.trim())} Greeting number $number.") })
}
```

`AtomicInteger`, not an `Int`, because two windows can click at the same moment. If you skipped
exercise 2, call `greetingFor(signals.name.trim())` instead.

</details>

### Exercise 4: Show the name as you type

Under the field, show what is in it, letter by letter as it is typed. The server is not asked:
`dataText` tells Datastar to keep an element's text equal to a signal.

<details><summary>Solution</summary>

In `helloForm`, after the `input`:

```kotlin
p {
    lt(Lt.paragraph())
    dataText(signal("name"))
}
```

`signal("name")` writes `$name`, which is how Datastar refers to the signal `name`.

Now break it on purpose. Write `dataText("$name")` instead. In a Kotlin string, `$name` means a
Kotlin value called `name`, and Kotlin finds one: the `name` attribute of the `form` around the
paragraph. kotlinx.html does not let you reach it from inside `p { }`, so the build fails, and the
message names `var name`. Add `val name = "Ada"` on the line above. Now it compiles, and the page
gets `data-text="Ada"`: Kotlin put its own `name` in the string, and Datastar never sees a signal.
Run the tests: `AnalysisTest` fails and names the line. With the Streamlord plugin in IntelliJ IDEA
or VS Code, the line is marked as you type it. Take both lines out again, and put `signal("name")`
back.

</details>

### Kotlin in this lesson

All in [`HelloController.kt`](src/main/kotlin/forgelord/hello/HelloController.kt):

- `class HelloController(private val streamlord: Streamlord)`. The constructor's parameters are
  written after the class name, and `val` makes each one a property of the object. Spring calls
  the constructor and passes the `Streamlord` (injection, above).
- `fun page(request: HttpServletRequest): String = page(...)`. A function whose body is one
  expression is written with `=` and no `return`. The `page(...)` after `=` is a different
  function with the same name, from `page/Page.kt`.
- `produces = [MediaType.TEXT_HTML_VALUE]`. An argument given by its name. `[...]` makes an array,
  and is only allowed in annotations.
- `main { h1 { ... } }`. kotlinx.html: each tag is a function, and the code in the braces after it
  writes what goes inside the tag. The braces are a lambda, a function without a name. When a
  lambda is the last argument, it is written after the parentheses, and when it is the only one,
  `()` is left out.
- `+"Hello Gallowmark"`. `+` is an operator kotlinx.html defines on strings: it adds the text
  inside the tag it is in.
- `private fun FlowContent.greeting(text: String)`. An extension function: it is called as if it
  were a method on `FlowContent`, kotlinx.html's type for a tag that can hold other tags, and that
  tag is `this` inside it.
- `request.cspNonce`. An extension property Streamlord adds to the servlet request. It gives this
  request's nonce: a one-time code that lets the page's own scripts run under the
  Content-Security-Policy.
- `data class HelloSignals(val name: String = "")`. A data class gets `equals`, `toString` and
  `copy` made for it. `= ""` is the value used when the JSON has no `name`.
- `if (name.isEmpty()) "Hello, stranger." else "Hello, $name."`. `if` is an expression: it gives a
  value, so it can be the whole body after `=`. `$name` in a string puts in the value of `name`.

## Lesson 2: A list and a command

<http://localhost:8080/notice> shows three quests, each with a "Ride out" button. Read
[`notice/NoticeController.kt`](src/main/kotlin/forgelord/notice/NoticeController.kt).

What is new since lesson 1: the cards come from a list, the URL says which quest was clicked, and
the server keeps a count. Click "Ride out" and the card shows the new count.

`rideOut` needs no signals, but calls `readSignals(streamlord)` anyway. It gives the signals as
plain JSON, with no class to put them in, and refuses a body that is not JSON with 415, as
`readSignalsOr` does. The refusal is why it is there: an HTML form on another site can send form
data or plain text, never JSON, so it cannot make a visitor ride out without knowing it.

Open the page in a second window, and click there. The first window still shows the old number,
until you reload it. The answer to a click goes only to the window that clicked.

### Exercise 5: A badge for a popular quest

When three or more have ridden out on a quest, its card shows a badge that says "Popular". The
test is in [`exercises/NoticeExercisesTest.kt`](src/test/kotlin/forgelord/exercises/NoticeExercisesTest.kt).

<details><summary>Solution</summary>

In `noticeCard`, after the paragraph with the count:

```kotlin
if (riders >= 3) {
    span { lt(Lt.badge(color = BadgeColor.SUCCESS)); +"Popular" }
}
```

Only the card changes. The count was already there: `riders` is the card's parameter.

</details>

### Kotlin in this lesson

All in [`NoticeController.kt`](src/main/kotlin/forgelord/notice/NoticeController.kt):

- `val NOTICES = listOf(...)`. A list that cannot be changed. It is declared outside the class, so
  it is one list for the whole app.
- `for (notice in NOTICES) { noticeCard(...) }`. A loop inside `main { }` writes one card for each
  quest.
- `NOTICES.associate { it.id to AtomicInteger() }` in `riders`. In a lambda with one parameter,
  `it` is that parameter. `a to b` makes a pair, and `associate` makes a map from the pairs.
- `@PathVariable id: String` in `rideOut`. The `{id}` part of `/notice/{id}/ride`.
- `NOTICES.find { it.id == id } ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)`. `find`
  gives a `Notice?`: a `Notice`, or `null` when none matches. `?:` gives what is on its right
  when what is on its left is `null`. After the line, Kotlin knows `notice` is not `null`.
- `riders.getValue(id)`. Looks up a key that must be there, and throws an exception if it is not.
  `riders[id]` would give `null` for a missing key.
- `when (count) { 0 -> ...; 1 -> ...; else -> ... }` in `ridersText`. Picks the first branch that
  matches, and gives its value.
- `"notice-${notice.id}"`. `${...}` is needed when what goes in the string is more than a plain
  name, such as `notice.id`.

## Lesson 3: Every window sees it

<http://localhost:8080/together> is lesson 2's list again. Open it in two windows and ride out in
one: both show the new count. Read
[`together/TogetherController.kt`](src/main/kotlin/forgelord/together/TogetherController.kt), and
compare it with lesson 2's.

What changed:

- **The count is a `MutableStateFlow`.** It holds a map from quest to count, and tells every open
  stream when the map is replaced.
- **Each page opens a stream as it loads, to `/together/board`.** The server holds that request
  open, and sends the cards down it at once, then again whenever the count changes.
- **"Ride out" answers `204 No Content`.** The answer has no HTML, because the new cards come down
  every window's stream, the clicking one's included.
- **The cards come from `cardList` in `components/`,** and the section around them from `shell`.
  Lesson 2 wrote its card by hand. A component is a function more than one page uses; the board
  in lesson 4 uses the same two. Inside `cardList`'s last lambda, `this` is a `FlowContent`, so
  `h3 { }` and the other tags work there.

### Exercise 6: Turn back

A "Turn back" button beside "Ride out" takes one rider off the quest, never below none, and every
open window shows the new count. The test is in
[`exercises/TogetherExercisesTest.kt`](src/test/kotlin/forgelord/exercises/TogetherExercisesTest.kt).

<details><summary>Solution</summary>

A second command beside `rideOut`:

```kotlin
@PostMapping("/together/{id}/turn-back")
@ResponseStatus(HttpStatus.NO_CONTENT)
fun turnBack(
    @PathVariable id: String,
    request: HttpServletRequest,
) {
    request.readSignals(streamlord)
    if (id !in riders.value) throw ResponseStatusException(HttpStatus.NOT_FOUND)

    riders.update { counts -> counts + (id to maxOf(0, counts.getValue(id) - 1)) }
}
```

And in `cards`, at the bottom of the file, after the "Ride out" button:

```kotlin
button {
    lt(Lt.button(variant = ButtonVariant.GHOST))
    type = ButtonType.button
    dataOnClick(post("/together/${quest.id}/turn-back"))
    +"Turn back"
}
```

Nothing else: the stream already sends the cards whenever `riders` changes.

</details>

### Kotlin in this lesson

All in [`TogetherController.kt`](src/main/kotlin/forgelord/together/TogetherController.kt):

- `MutableStateFlow(...)`, `riders.value` and `riders.update { ... }`. From kotlinx.coroutines.
  `value` is what the flow holds now, and `update` replaces it. You write no coroutine code here:
  `sendLatest`, in the endpoint `board`, reads the flow for you.
- `counts + (id to counts.getValue(id) + 1)` in `rideOut`. A map plus a pair is a new map, with
  that key's value replaced. The old map is not changed. `+` is done before `to`, so the pair is
  `id` and the count plus one.
- `id !in riders.value`. `in` on a map asks whether the key is there.
- `@ResponseStatus(HttpStatus.NO_CONTENT)` on `rideOut`. Makes Spring answer 204 instead of 200.
  The function returns nothing, so the body is empty.
- `cardList(QUESTS, cardId = { "together-${it.id}" }) { quest -> ... }`. Two lambdas: one passed
  by name, and the last one after the parentheses. `quest ->` names the lambda's parameter instead
  of `it`.
- `const val TOGETHER_BOARD_ID`. A constant, known when the code is compiled.
- `fun <T> FlowContent.cardList(items: List<T>, ...)` in
  [`CardList.kt`](src/main/kotlin/forgelord/components/cardlist/CardList.kt). `<T>` is a type
  parameter: the list can hold any type, and `cardId` and the card get items of that type.

## Lesson 4: The board

<http://localhost:8080> is the quest board at Hookford. Ride out on a quest, or nail up your own.
Open it in two windows, and both follow, as in lesson 3.

What is new: the board reads its quests from the Ledger, a separate service it calls over HTTP.
Here the Ledger runs in the same app, at <http://localhost:8080/ledger>, where you can open it,
wipe it or close it. The Ledger can be slow, empty or closed, so the board has four states:
loading, with quests, empty, and closed. The board asks the Ledger again every two seconds while
any window has it open.

The page, the stream and the commands work as in lesson 3.

### Where the code is

Lesson 4 is spread over several folders. Each box is a folder under `src/main/kotlin/forgelord/`,
and an arrow means "uses": `quests/web` imports from `components`.

```mermaid
flowchart TD
    subgraph app ["The app"]
        hello["hello"]
        notice["notice"]
        together["together"]
        web["quests/web"]
        quests["quests"]
        client["quests/client"]
        components["components"]
        page["page"]
        configuration["configuration"]
    end
    subgraph source ["A separate service"]
        ledger["ledger"]
    end
    hello --> page
    notice --> page
    together --> components
    together --> page
    web --> components
    web --> page
    web --> quests
    quests <--> client
    components --> page
    client -. HTTP .-> ledger
    ledger --> components
    ledger --> page
```

Each lesson's folder uses only `page/` and `components/`, never another lesson's. `page/` uses
none of the others, and `components/` knows nothing of quests, so both can go into
another app as they are. Nothing in the app imports `ledger/`: the board reaches it over HTTP, the
way it would reach a real service. It runs in the same app only so there is one thing to start.
`quests` and `quests/client` use each other, since the service calls the client and the client
turns the Ledger's JSON into the board's own types.
`configuration/` sets up Streamlord, the Content-Security-Policy, the Ledger's address and the
scope the board asks the Ledger in, and Spring hands them to the rest.

```text
src/
├── main/
│   ├── kotlin/forgelord/
│   │   ├── hello/          Lesson 1, in one file
│   │   ├── notice/         Lesson 2, in one file
│   │   ├── together/       Lesson 3, in one file
│   │   ├── quests/         the board's types, and the service that reads them
│   │   │   ├── web/        the controller, the views and the form's rules
│   │   │   └── client/     the Ledger over HTTP
│   │   ├── components/     what every page can use: field, card list, toast and more
│   │   ├── page/           the page around everything: LO-TEK, the CSS, the scripts
│   │   ├── configuration/  Streamlord, the Content-Security-Policy, the Ledger's address
│   │   └── ledger/         the stand-in for a real source service
│   └── resources/static/   datastar.js, and forgelord.js, which loads LO-TEK
└── test/kotlin/forgelord/
    └── exercises/          the exercises, switched off
```

Read it in this order:

| Where | What |
| --- | --- |
| `quests/web/ViewController.kt` | The routes: the page, the board's stream, and the two commands |
| `quests/web/QuestView.kt` | The HTML: the page, and the board in each of its four states |
| `quests/web/QuestFormView.kt` | The HTML of the form, with the server's verdict on each field |
| `quests/web/QuestForm.kt` | The rules for a new quest, on the server and nowhere else |
| `quests/QuestResult.kt` | The four states, what the Ledger answered, and what became of a write |
| `quests/QuestService.kt` | The board as a `StateFlow`, as in lesson 3, but read from the Ledger |
| `quests/Quest.kt` | A quest as the board knows it |
| `quests/client/` | The Ledger over HTTP: its JSON turned into a `Quest`, its status codes into the board's words |
| `ledger/` | The Ledger itself. Exercise 9 changes it, as you would ask the team that owns a real one |

The exercises are in
[`exercises/BoardExercisesTest.kt`](src/test/kotlin/forgelord/exercises/BoardExercisesTest.kt).

### Exercise 7: Four states

Keep the board open in one window and `/ledger` in another. Wipe the Ledger, close it, open it.
There is no test: you are done when you have seen the board loading, with quests, empty, and closed.
Nail up a quest while the Ledger is closed, and see what you get.

### Exercise 8: Keep the task short

"What has to be done" may be at most 300 characters. Longer, and the form says so at that field.
Only `QuestForm.kt` changes.

<details><summary>Solution</summary>

Beside the other limits:

```kotlin
private const val TEXT_MAX = 300
```

In `check`, in place of the line that checks `trimmedText`:

```kotlin
when {
    trimmedText.isEmpty() -> add(Fault(QuestForm::text, "Say what has to be done."))
    trimmedText.length > TEXT_MAX -> add(Fault(QuestForm::text, "No more than $TEXT_MAX characters."))
}
```

The form already shows each `Fault` at its field, and in the summary above the form.

</details>

### Exercise 9: A name already on the post

Nailing up a quest with the name of one already on the post shows "<the name> is already on the
post." at the name field, and nothing goes up. Capital letters do not count: "a tenth of the wood"
is the same name as "A Tenth of the Wood".

<details><summary>Solution</summary>

The Ledger owns the quests, so the Ledger refuses the name, with `409 Conflict`. If the board
checked first, two people could still nail up the same name at the same moment, between the
board's check and the Ledger's write.

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

Add `TAKEN` to `WriteResult` in `QuestResult.kt`. The client is where a status code becomes one
of the board's words, so in `QuestClient.write`, catch the conflict before `Exception`. `Conflict`
is a kind of `Exception`, and Kotlin uses the first `catch` that matches:

```kotlin
} catch (_: HttpClientErrorException.Conflict) {
    WriteResult.TAKEN
} catch (e: Exception) {
```

`QuestService` hands the result on as it is, and needs no change.

Now build. The build fails twice in `ViewController`: neither `when` over the result covers every
case any more. In `nail`, add the branch:

```kotlin
WriteResult.TAKEN ->
    patchElements(fragment { questForm(emptySignals, listOf(Fault(QuestForm::name, "${checked.quest.name} is already on the post."))) })
```

A ride is never `TAKEN`, so in `rideOut` it joins the branch that says the least:

```kotlin
WriteResult.CLOSED, WriteResult.TAKEN -> "The Ledger did not take it."
```

</details>

### Kotlin in this lesson

- `sealed interface QuestResult` in [`QuestResult.kt`](src/main/kotlin/forgelord/quests/QuestResult.kt),
  with `data object Loading` and `data class Found`. A sealed type has a fixed set of subtypes, all
  in the same package. A `data object` is a type with exactly one value, whose `toString` gives
  its name.
- `when (result) { QuestResult.Loading -> ...; is QuestResult.Found -> ... }` in `boardView`. `is`
  checks the type, and in that branch `result` has it: `result.quests` works. Leave a state out,
  and the build fails.
- `enum class WriteResult { DONE, CLOSED, GONE }`. A fixed set of values.
- `object QuestPageClasses` and `val questPageCss by lazy { ... }` in
  [`QuestPageClasses.kt`](src/main/kotlin/forgelord/quests/web/QuestPageClasses.kt). An `object`
  is a class with exactly one instance. `by lazy` works the value out the first time it is read.
- `buildList { add(...) }` in `QuestForm.check`. Makes a list by adding to it inside the braces.
- `QuestForm::name` in `Fault(QuestForm::name, ...)`. A reference to the property itself, not its
  value. The form gets the field's id and its signal from it, so the name is written once, in the
  class, and a field that does not exist does not compile.
- `try { ... } catch (_: HttpClientErrorException.NotFound) { ... }` in `QuestClient.write`. `try`
  gives a value, as `if` does. `_` in place of a name says the exception is not used.
- The `flow { ... }.stateIn(...)` in `QuestService.board` is coroutines. You can read the rest
  without it: `board` is a `StateFlow` as in lesson 3, and this code fills it by asking the Ledger.

## When you build on it

The lessons do not need this section.

**Rules, and the tests that hold them.** Markup is kotlinx.html, never HTML in strings. A CSS class
is a constant in a `…Classes` object or comes from LO-TEK, never a free string. `unsafe { }` is used
once, for the page's CSS. Seven tests hold the structure:

- `ArchitectureTest` has one table of what each folder uses of the others, and checks the code
  against it. A new folder needs a row before it can use anything.
- `ComponentStylesTest` checks that the components' CSS names no domain, and that every file of it
  is on the page.
- `ClassesTest` checks that every class constant is in its CSS file, and the other way round.
- `UnsafeTest` checks that `unsafe { }` is where it may be.
- `NewQuestTest` checks that a `NewQuest` is made only by the form's rules.
- `InstructionsTest` checks that `CLAUDE.md` imports every instruction file.
- `ReadmeTest` checks that the diagram and the tree in lesson 4 name every folder, and that the
  diagram's arrows are the ones in `ArchitectureTest`.

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
