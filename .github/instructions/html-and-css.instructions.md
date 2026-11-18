---
name: "Server-rendered HTML and CSS"
description: "Use when: writing or changing HTML, components, views, CSS or CSS classes. Keywords: kotlinx.html, FlowContent, Content, component, LO-TEK, Lt, lt(), Classes, ComponentStyles, @layer, assertLotek, checked."
applyTo: "**/src/{main,test}/kotlin/**/{components,web,page,ledger}/**/*.kt, **/src/main/kotlin/**/*.css"
---
# Server-rendered HTML and CSS

Every page is rendered on the server. Markup is written with kotlinx.html and styled with LO-TEK plus as little CSS of our own as possible.

## Markup

- Write markup with the kotlinx.html DSL. Never HTML in strings, `$$"""..."""` or `@Language("HTML")`.
- A component is an extension on `FlowContent`. Markup passed around is `Content` (`FlowContent.() -> Unit`, in `page/Content.kt`). `fragment { }` renders a piece of a page for a patch.
- Write text with `+text` and attributes with DSL properties or `attributes["..."]`. Datastar attributes always use Streamlord's functions; see `streamlord-datastar.instructions.md`. kotlinx.html escapes both, so text from the Ledger or the request needs no extra handling.
- `unsafe { }` is forbidden. The only exception is the CSS in `page/Page.kt`, which refuses `</`. JavaScript goes in a file under `static/`, as `forgelord.js` does, never inline.

```kotlin
fun FlowContent.statusMessage(status: Status, text: String) {
    div {
        lt(Lt.alert(color = if (status == Status.ERROR) AlertColor.DANGER else AlertColor.INFO))
        attributes["role"] = if (status == Status.ERROR) "alert" else "status"
        p { +text }
    }
}
```

## CSS classes

- Never write a CSS class as a free string. Use LO-TEK's builders for LO-TEK, and constants in an `object` for our own CSS.
- LO-TEK classes and attributes come from `Lt` in `no.lotek`, applied with `lt(…)` from `no.lotek.html`: `p { lt(Lt.paragraph(size = Size.SMALL)) }`. Use the builder's enum options for variants instead of `attributes[...]`. Parts the template writes itself are on the builder: `Lt.card.title`.
- A component with its own CSS has three files side by side: `CardList.kt`, `CardList.css` and `CardListClasses.kt`. A page's own CSS follows the same pattern: `QuestPage.css` and `QuestPageClasses.kt`.
- Combine our classes with LO-TEK by giving our class to the tag and adding LO-TEK with `lt(…)`: `button(classes = QuestPageClasses.SUBMIT) { lt(Lt.button()) }`.
- When you add or change a class constant, update the CSS. `ClassesTest` compares every `…Classes` object with its CSS file, and fails if an object is missing from its list.

## CSS

- Prefer LO-TEK classes and variants. Add CSS only for what LO-TEK cannot do, with LO-TEK tokens (`--lt-*`), never hard-coded colours or pixels.
- The layers are `lotek, forgelord-components, forgelord-page`, declared in `page/Page.kt`. Component CSS goes in `@layer forgelord-components`; a page's CSS in `@layer forgelord-page`. CSS outside a layer would beat all of them, so there is none.
- Register each new component CSS file in `ComponentStyles.FILES`, relative to `components/`. `ComponentStylesTest` fails if one is missing.
- The CSS reaches the page in one `<style>` that carries the CSP nonce. Do not add `<link>`s for our own CSS.

## Component boundaries

- `components/` is for UI another page could use as it is: status message, field, error summary, card list, shell, toast. It must not know about a domain or `configuration/`.
- What is the same for every page (the page root, `Content`, `fragment { }`, `stylesheet()`) lives in `page/`, which knows nothing about components or domains.
- Domain-specific HTML, texts and URLs live in `<domain>/web/`.

## Tests

- Test components with a type unrelated to the domain, as `ComponentsTest` does with relays, and render them with `checked { }`. It runs `ElementsGuard` and `assertLotek` on a whole page, so every class, element and `data-*` value must exist, and every label and error text must point to a field.
- Read rendered markup with Jsoup through `dom(html)` in `Dom.kt`.
