---
name: "Backend architecture"
description: "Use when: organizing Kotlin code, adding a page or a domain, or deciding where a class belongs."
applyTo: "**/src/{main,test}/kotlin/**/*.kt"
---
# Backend architecture

Keep the structure proportional to the app; do not add packages or layers for code it does not need. Start with one package for a domain and add files only as responsibilities arise. Forgelord looks like this:

```text
src/main/kotlin/forgelord/
├── Main.kt
├── configuration/                 shared Spring configuration, filters, and properties
├── page/                          the page root, Content, fragment { }, stylesheet(), signals by property; widget/ in widget apps
├── components/                    reusable UI components, independent of any domain
├── lesson1hello/
│   ├── HelloController.kt         lesson 1, all its Kotlin: the page, one command, the view
│   └── HelloPage.css, HelloPageClasses.kt
├── lesson2notice/
│   ├── NoticeController.kt        lesson 2, all its Kotlin: a list, and a command that answers with a card
│   └── NoticePage.css, NoticePageClasses.kt
├── lesson3together/
│   ├── TogetherController.kt      lesson 3, all its Kotlin: a StateFlow, a stream, and a command that answers 204
│   └── TogetherPage.css, TogetherPageClasses.kt
├── lesson4quests/                 lesson 4, the board
│   ├── QuestService.kt            loads the board from the Ledger and keeps it as one StateFlow
│   ├── QuestResult.kt             the board's states (Loading, Found, Empty, Unavailable), and Reading and WriteResult
│   ├── Quest.kt                   the domain's own types
│   ├── client/
│   │   ├── QuestClient.kt         calls the Ledger with RestClient, and answers in the board's words
│   │   └── QuestDto.kt            the Ledger's JSON, mapped to Quest at the border
│   └── web/
│       ├── ViewController.kt      the page, the board's stream, and the commands
│       ├── QuestView.kt           the page and the four board states
│       ├── QuestFormView.kt       the form
│       ├── QuestForm.kt           the form's signals and the rules for a new quest
│       └── QuestPage.css, QuestPageClasses.kt
└── ledger/                        a stand-in source service, reached only over HTTP
```

A lesson's folder has the lesson's number in its name, so the folders sort in the order the README teaches them. A page that is not a lesson is named after its domain, with no number. Lessons 1 to 3 each have a CSS file with no rules and an empty `…PageClasses` object, already passed to `page(...)` and listed in `ClassesTest`, so a reader can try a rule without setting anything up.

Keep the controller and the domain's views together in `<domain>/web/`; `Controller` describes a Spring role, not a package. A source client and its DTOs belong in `client/`. The client answers in the domain's own types (`Reading`, `WriteResult`) and throws nothing: status codes, timeouts and HTTP exceptions stop there, and the service never catches one. The service loads and coordinates domain data; the controller and the views do not.

Put only UI components that another page could use without changes in `components/`. Keep generic rendering and layout there; keep domain-specific labels, URLs, and formatting in `<domain>/web/`. Infrastructure that is the same for every page, such as the page root and `Content`, belongs in `page/`.

Dependencies point toward the web boundary: `<domain>/web/` may use the domain service and components; the service may use the client; `components/` may use `page/`. Neither `components/` nor `page/` may depend on a domain or on `configuration/`. `configuration/` uses `page/` for one thing: in development it sets the script that reloads an open page. `lesson4quests/` never refers to `ledger/`, and `lesson4quests/` and `lesson4quests/client/` never refer to `lesson4quests/web/`. Do not move code to `components/` merely to split up a view.

Mirror production packages under `src/test/kotlin/`. Test the client against `MockRestServiceServer` (see `LedgerStub`), the service's states, the components with data unrelated to any domain, and the controller through `MockMvcTester`, reading the answer back with `datastarEvents`. `LiveBoardTest` holds a stream open on a real server.

## Enforce package boundaries with tests

Package boundaries are executable rules. They are written once, as the table `MAY_USE` in `ArchitectureTest`: one row per folder, with the folders it uses. `lesson4quests`, `lesson4quests/client` and `lesson4quests/web` have a row each. The test reads the source under `src/main/kotlin/` and fails with the file and line.

- A folder may use what its row names and nothing else. A new folder has no row, so the test fails until you add one.
- A row names only what its folder really uses. When the last use goes, take it out of the row.
- The README's diagram draws the same arrows, and `ReadmeTest` fails when the two differ. Change the table and the diagram together.
- `ComponentStylesTest`: component CSS names no domain; every CSS file under `components/` is in `ComponentStyles.FILES`.
- `UnsafeTest`: `unsafe { }` appears once, in `page/Page.kt`.

Run `./gradlew test` after changing these rules; a boundary violation fails the build.
