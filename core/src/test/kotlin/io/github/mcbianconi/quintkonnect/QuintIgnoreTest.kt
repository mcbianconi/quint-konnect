@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.annotations.QuintIgnore
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Serializable
private data class CounterWithIgnoredLabel(val count: Long, @QuintIgnore val label: String)

@Serializable
private data class CounterWithLabel(val count: Long, val label: String)

@Serializable
private data class Nested(val counter: CounterWithIgnoredLabel)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("tag")
private sealed class Move {
    @Serializable
    @SerialName("Played")
    data class Played(val by: String, @QuintIgnore val timestamp: Long) : Move()
}

class QuintIgnoreTest {

    private class FakeDriver : Driver {
        override fun step(step: Step) {}
    }

    private class TypedStateOf<S : Any>(
        serializer: KSerializer<S>,
        private val driverValue: S,
        private val compare: (String, String, String) -> Boolean? = { _, _, _ -> null },
    ) : TypedState<FakeDriver, S>(serializer) {
        override fun extractFromDriver(driver: FakeDriver): S = driverValue
        override fun compareField(path: String, spec: String, impl: String): Boolean? = compare(path, spec, impl)
    }

    private fun specCounter(count: Long, label: String): ItfValue = ItfValue.Record(
        linkedMapOf("count" to ItfValue.Num(count), "label" to ItfValue.Str(label)),
    )

    @Test
    fun `a mismatch on an ignored field does not fail the check`() {
        val state = TypedStateOf(CounterWithIgnoredLabel.serializer(), CounterWithIgnoredLabel(1, "impl-label"))

        state.check(FakeDriver(), specCounter(1, "spec-label"))
    }

    @Test
    fun `the same mismatch fails without the annotation`() {
        val state = TypedStateOf(CounterWithLabel.serializer(), CounterWithLabel(1, "impl-label"))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), specCounter(1, "spec-label"))
        }

        assertTrue(thrown.message!!.contains("label: spec=\"spec-label\", impl=\"impl-label\""))
    }

    @Test
    fun `a mismatch on a non-ignored field still fails and never mentions the ignored field`() {
        val state = TypedStateOf(CounterWithIgnoredLabel.serializer(), CounterWithIgnoredLabel(2, "impl-label"))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), specCounter(1, "spec-label"))
        }

        assertTrue(thrown.message!!.contains("count: spec=1, impl=2"))
        assertFalse(thrown.message!!.contains("label"))
    }

    @Test
    fun `an ignored field nested inside a record is skipped`() {
        val spec = ItfValue.Record(
            linkedMapOf("counter" to specCounter(1, "spec-label")),
        )
        val state = TypedStateOf(Nested.serializer(), Nested(CounterWithIgnoredLabel(1, "impl-label")))

        state.check(FakeDriver(), spec)
    }

    @Test
    fun `an ignored field inside a sealed payload is skipped`() {
        val spec = ItfValue.Record(
            linkedMapOf(
                "tag" to ItfValue.Str("Played"),
                "by" to ItfValue.Str("p1"),
                "timestamp" to ItfValue.Num(1),
            ),
        )
        val state = TypedStateOf(Move.serializer(), Move.Played(by = "p1", timestamp = 999))

        state.check(FakeDriver(), spec)
    }

    @Test
    fun `compareField returning true suppresses a real diff`() {
        val state = TypedStateOf(
            CounterWithLabel.serializer(),
            CounterWithLabel(1, "impl-label"),
            compare = { path, _, _ -> if (path == "label") true else null },
        )

        state.check(FakeDriver(), specCounter(1, "spec-label"))
    }

    @Test
    fun `compareField returning false forces a diff on an otherwise equal field`() {
        val state = TypedStateOf(
            CounterWithLabel.serializer(),
            CounterWithLabel(1, "same"),
            compare = { path, _, _ -> if (path == "label") false else null },
        )

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), specCounter(1, "same"))
        }

        assertTrue(thrown.message!!.contains("label: spec=\"same\", impl=\"same\""))
    }

    @Test
    fun `compareField receives the documented path for a nested field`() {
        val seen = mutableListOf<String>()
        val state = TypedStateOf(
            Nested.serializer(),
            Nested(CounterWithIgnoredLabel(1, "impl-label")),
            compare = { path, _, _ -> seen += path; null },
        )

        state.check(FakeDriver(), ItfValue.Record(linkedMapOf("counter" to specCounter(1, "spec-label"))))

        assertTrue(seen.contains("counter.count"))
        assertTrue(seen.contains("counter"))
        assertTrue(seen.contains(""))
    }
}
