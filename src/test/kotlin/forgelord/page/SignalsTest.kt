package forgelord.page

import kotlinx.html.input
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SignalsTest {
    private data class Inner(val name: String = "")

    private data class Outer(val inner: Inner = Inner(), val count: Int = 0)

    @Test
    fun `a property is the signal of the same name`() {
        assertEquals("count", signalPath(Outer::count))
        assertEquals("\$count", signal(Outer::count))
    }

    @Test
    fun `a property inside another is a path with a dot`() {
        assertEquals("inner.name", signalPath(Outer::inner, Inner::name))
    }

    @Test
    fun `a field is bound to the property's signal`() {
        assertContains(fragment { input { dataBind(Outer::count) } }, """data-bind="count"""")
        assertContains(fragment { input { dataBind(Outer::inner, Inner::name) } }, """data-bind="inner.name"""")
    }
}
