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
├── page/                          the page root, Content, fragment { }, stylesheet(); widget/ in widget apps
├── hello/
│   └── HelloController.kt         lesson 1 in one file: the page, one command, the view
├── notice/
│   └── NoticeController.kt        lesson 2 in one file: a list, and a command that answers with a card
├── together/
│   └── TogetherController.kt      lesson 3 in one file: a StateFlow, a stream, and a command that answers 204
├── components/                    reusable UI components, independent of any domain
├── quests/
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

Keep the controller and the domain's views together in `<domain>/web/`; `Controller` describes a Spring role, not a package. A source client and its DTOs belong in `client/`. The client answers in the domain's own types (`Reading`, `WriteResult`) and throws nothing: status codes, timeouts and HTTP exceptions stop there, and the service never catches one. The service loads and coordinates domain data; the controller and the views do not.

Put only UI components that another page could use without changes in `components/`. Keep generic rendering and layout there; keep domain-specific labels, URLs, and formatting in `<domain>/web/`. Infrastructure that is the same for every page, such as the page root and `Content`, belongs in `page/`.

Dependencies point toward the web boundary: `<domain>/web/` may use the domain service and components; the service may use the client; `components/` may use `page/`. Neither `components/` nor `page/` may depend on a domain or on `configuration/`. `quests/` never refers to `ledger/`. Do not move code to `components/` merely to split up a view.

Mirror production packages under `src/test/kotlin/`. Test the client against `MockRestServiceServer` (see `LedgerStub`), the service's states, the components with data unrelated to any domain, and the controller through `MockMvcTester`, reading the answer back with `datastarEvents`. `LiveBoardTest` holds a stream open on a real server.

## Enforce package boundaries with tests

Package boundaries are executable rules. When a package is added, moved, or its allowed dependencies change, update its boundary test. The tests read the source under `src/main/kotlin/` and fail with the file and line.

- `ComponentBoundaryTest`: `components/` refers to no domain or lesson and not to `configuration/`; component CSS names no domain; every CSS file under `components/` is in `ComponentStyles.FILES`.
- `PageBoundaryTest`: `page/` refers to no domain or lesson, not to `configuration/`, and not to `components/`.
- `QuestsBoundaryTest`: `quests/` never refers to `ledger/`.
- `HelloBoundaryTest` and `NoticeBoundaryTest`: `hello/` and `notice/` use `page/` and nothing else of the app, so each can be read and copied on its own.
- `TogetherBoundaryTest`: `together/` uses `page/` and `components/`, and no other lesson or domain.
- `UnsafeTest`: `unsafe { }` appears once, in `page/Page.kt`.

Run `./gradlew test` after changing these rules; a boundary violation fails the build.
