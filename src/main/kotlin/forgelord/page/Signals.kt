package forgelord.page

import io.github.markusaugust.streamlord.html.dataBind
import io.github.markusaugust.streamlord.html.signal
import kotlinx.html.HTMLTag
import kotlin.reflect.KProperty1

/**
 * A signal, named by the property the server reads it into.
 *
 * The page names a signal and the server reads it into a class, and the two must agree.
 * `dataBind("name")` is checked by nobody: misspell it, and the field is bound to a signal the
 * server never reads. `dataBind(HelloSignals::name)` does not compile unless the class has it.
 */
fun signalPath(property: KProperty1<*, *>): String = property.name

/** A signal inside another: `signalPath(QuestSignals::quest, QuestForm::name)` is `quest.name`. */
fun <T> signalPath(
    outer: KProperty1<*, T>,
    inner: KProperty1<T, *>,
): String = "${outer.name}.${inner.name}"

/** Binds a field to the signal [property] is read from. */
fun HTMLTag.dataBind(property: KProperty1<*, *>) = dataBind(signalPath(property))

/** Binds a field to a signal inside another. [inner] has to be a property of what [outer] holds. */
fun <T> HTMLTag.dataBind(
    outer: KProperty1<*, T>,
    inner: KProperty1<T, *>,
) = dataBind(signalPath(outer, inner))

/** The signal in an expression: `signal(HelloSignals::name)` writes `$name`. */
fun signal(property: KProperty1<*, *>): String = signal(signalPath(property))
