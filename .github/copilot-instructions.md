# Forgelord

A small server-rendered web app: Kotlin, Spring Boot 4 with Spring MVC, kotlinx.html,
[Streamlord](https://streamlord.sobernetics.no) for Datastar, and
[LO-TEK](https://lo-tek.sobernetics.no) for the design. It is the quest board at Hookford: a page
that shows quests from a source service, the Ledger, and keeps every open page up to date over one
stream. It exists to show how an app on this stack is built, so keep it small and keep it an example
someone could copy.

## Commands

- `./gradlew check` runs every test, including the structure tests, and then `SolutionsTest`: it pastes the README's solutions into a copy of the project and runs the exercises there. Run it before you say you are done. `./gradlew test` is the same without `SolutionsTest`, and a minute faster.
- `./gradlew bootRun` starts the app on <http://localhost:8080>. `--args='--server.port=8091'` if 8080 is taken.
- `./gradlew -t classes` beside it compiles on every save. The app restarts, and an open page reloads itself.
- `/hello` is lesson 1: one field, one round trip. `/notice` is lesson 2: a list and a command. `/together` is lesson 3: the same list over a stream. `/` is lesson 4, the board. `/ledger` switches the Ledger between open, empty and closed.

## Where things are

```text
src/main/kotlin/forgelord/
├── Main.kt
├── configuration/   Spring configuration: the Streamlord bean, the CSP filter, the Ledger's HTTP client, the coroutine scope, reloading in development
├── page/            the page around everything, Content, fragment { }, stylesheets, signals named by property (widget/ in widget apps)
├── lesson1hello/    lesson 1, on its own: page/, LO-TEK and Streamlord, nothing else
├── lesson2notice/   lesson 2, on its own, like lesson1hello/
├── lesson3together/ lesson 3: page/ and components/, nothing else of the app
├── lesson4quests/   lesson 4, the board: QuestService, QuestResult, client/ for the Ledger, web/ for HTTP and views
├── components/      UI components another page could use as they are
└── ledger/          a stand-in for a source service; lesson4quests/ reaches it over HTTP only
```

A lesson's folder has the lesson's number in its name, so the folders sort in the order the README
teaches them. A page that is not a lesson is named after what it is about, with no number.

```text
src/test/kotlin/forgelord/exercises/
└── Lesson1HelloExercisesTest.kt … Lesson5WatchExercisesTest.kt   one file for each lesson
```

The rules for each part are in `.github/instructions/`:

- `widget-architecture.instructions.md`: packages, what may depend on what, and the test that holds it.
- `html-and-css.instructions.md`: markup with kotlinx.html, CSS classes, layers, components.
- `streamlord-datastar.instructions.md`: streams, commands, signals, CSP.

## Rules that hold everywhere

- Markup is kotlinx.html. Never HTML in strings, and never `unsafe { }` outside `page/Page.kt`.
- A CSS class is a constant in an `…Classes` object or comes from LO-TEK's `Lt`, never a free string.
- Datastar attributes come from Streamlord's functions (`dataOnClick`, `dataBind`, `post(…)`), never
  written by hand. A signal is named by its property, `dataBind(HelloSignals::name)`, never by a string.
- The board shows four states: loading, data, empty and error. A failure is a state, not an exception.
- A boundary is a test. When you add or move a package, update `MAY_USE` in `ArchitectureTest` and the README's diagram.
- Write in English, in short sentences. Comments say why, not what.
- `src/test/kotlin/forgelord/exercises/` holds the README's exercises as `@Disabled` tests. Leave
  them disabled on master; the solutions in the README must make them pass. `SolutionsTest` holds
  that: every code block in a solution has a `<!-- solution: OPERATION FILE :: ANCHOR -->` comment
  on the line above it that says where the code goes, and `SolutionsTest.kt` lists the operations.
  When you change a solution, or the code its anchor names, keep the words for the reader and the
  comment saying the same place.
- An exercise has the same number and title in its README heading and in `@Disabled("Exercise 9: …")`,
  and its `**Test:**` line links to the test. A new exercise moves the numbers after it in both
  places. `ReadmeTest` fails when they differ.
