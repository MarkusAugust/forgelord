package forgelord.components.cardlist

import kotlinx.html.FlowContent
import kotlinx.html.article
import kotlinx.html.id
import kotlinx.html.li
import kotlinx.html.ul
import no.lotek.ListVariant
import no.lotek.Lt
import no.lotek.html.lt

/**
 * A list of cards, one per item.
 *
 * The component knows nothing about the items. The caller says what id each card has and what
 * goes in it.
 *
 * @param id Each card's id. Make it from a key in the data, never the index, so a morph finds the
 *   same card on every render.
 */
fun <T> FlowContent.cardList(
    items: List<T>,
    id: (T) -> String,
    card: FlowContent.(T) -> Unit,
) {
    ul(CardListClasses.LIST) {
        lt(Lt.list(variant = ListVariant.PLAIN))
        for (item in items) {
            li {
                article {
                    lt(Lt.card())
                    this.id = id(item)
                    card(item)
                }
            }
        }
    }
}
