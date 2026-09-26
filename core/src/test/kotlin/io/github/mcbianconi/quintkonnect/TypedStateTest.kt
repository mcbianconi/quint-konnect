package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.ItfValue
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonClassDiscriminator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

@Serializable
private data class Counter(val count: Long, val label: String)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("tag")
private sealed class Cell {
    @Serializable
    @SerialName("X")
    data class X(val value: Long) : Cell()

    @Serializable
    @SerialName("O")
    object O : Cell()
}

@Serializable
private data class Board(val cells: Map<List<Long>, String>)

@Serializable
private data class Bag(val items: Set<Long>)

@Serializable
private data class Nullable(val inner: Long?)

@Serializable
private data class BigCounter(val n: @Contextual BigInteger)

class TypedStateTest {

    private class FakeDriver : Driver {
        override fun step(step: Step) {}
    }

    private class TypedStateOf<S : Any>(
        private val serializer: kotlinx.serialization.KSerializer<S>,
        private val driverValue: S,
    ) : TypedState<FakeDriver, S>(serializer) {
        override fun extractFromDriver(driver: FakeDriver): S = driverValue
    }

    private class CounterState(private val driverValue: Counter) : TypedState<FakeDriver, Counter>(Counter.serializer()) {
        override fun extractFromDriver(driver: FakeDriver): Counter = driverValue
    }

    private fun specRecord(count: ItfValue, label: String): ItfValue =
        ItfValue.Record(linkedMapOf("count" to count, "label" to ItfValue.Str(label)))

    @Test
    fun `passes when spec and driver state are equal`() {
        val state = CounterState(Counter(count = 1, label = "a"))
        state.check(FakeDriver(), specRecord(ItfValue.Num(1), "a"))
    }

    @Test
    fun `throws with a field-level diff naming the differing field on a mismatch`() {
        val state = CounterState(Counter(count = 2, label = "a"))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), specRecord(ItfValue.Num(1), "a"))
        }

        assertTrue(thrown.message!!.contains("count: spec=1, impl=2"))
        assertFalse(thrown.message!!.contains("label"))
    }

    @Test
    fun `keeps the full values available in the exception's cause`() {
        val state = CounterState(Counter(count = 2, label = "a"))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), specRecord(ItfValue.Num(1), "a"))
        }

        val cause = thrown.cause!!.message!!
        assertTrue(cause.contains("-Counter(count=1, label=a)"))
        assertTrue(cause.contains("+Counter(count=2, label=a)"))
    }

    @Test
    fun `decodes the spec value through the configured serializer before comparing`() {
        val state = CounterState(Counter(count = 5, label = "x"))

        state.check(FakeDriver(), specRecord(ItfValue.BigInt("5"), "x"))
    }

    @Test
    fun `decoding mismatch through the spec value is still caught`() {
        val state = CounterState(Counter(count = 5, label = "x"))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), specRecord(ItfValue.BigInt("6"), "x"))
        }

        assertTrue(thrown.message!!.contains("count: spec=6, impl=5"))
    }

    @Test
    fun `reports a differing value at a tuple-keyed map entry`() {
        val spec = ItfValue.Record(
            linkedMapOf(
                "cells" to ItfValue.Map(
                    listOf(ItfValue.Tup(listOf(ItfValue.Num(1), ItfValue.Num(2))) to ItfValue.Str("X")),
                ),
            ),
        )
        val state = TypedStateOf(Board.serializer(), Board(mapOf(listOf(1L, 2L) to "O")))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), spec)
        }

        assertTrue(thrown.message!!.contains("cells.(1, 2): spec=\"X\", impl=\"O\""))
    }

    @Test
    fun `reports a missing and an extra key in a map`() {
        val spec = ItfValue.Record(
            linkedMapOf(
                "cells" to ItfValue.Map(
                    listOf(ItfValue.Tup(listOf(ItfValue.Num(1), ItfValue.Num(1))) to ItfValue.Str("X")),
                ),
            ),
        )
        val state = TypedStateOf(Board.serializer(), Board(mapOf(listOf(2L, 2L) to "O")))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), spec)
        }

        assertTrue(thrown.message!!.contains("cells.(1, 1): missing in impl (spec=\"X\")"))
        assertTrue(thrown.message!!.contains("cells.(2, 2): extra in impl (impl=\"O\")"))
    }

    @Test
    fun `reports a missing and an extra element in a set, ignoring order`() {
        val spec = ItfValue.Record(
            linkedMapOf("items" to ItfValue.Set(listOf(ItfValue.Num(1), ItfValue.Num(2)))),
        )
        val state = TypedStateOf(Bag.serializer(), Bag(setOf(2L, 3L)))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), spec)
        }

        assertTrue(thrown.message!!.contains("missing in impl (1)"))
        assertTrue(thrown.message!!.contains("extra in impl (3)"))
    }

    @Test
    fun `a set in different insertion order is not a diff`() {
        val spec = ItfValue.Record(
            linkedMapOf("items" to ItfValue.Set(listOf(ItfValue.Num(1), ItfValue.Num(2)))),
        )
        val state = TypedStateOf(Bag.serializer(), Bag(setOf(2L, 1L)))

        state.check(FakeDriver(), spec)
    }

    @Test
    fun `reports a differing sealed tag`() {
        val spec = ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("O")))
        val state = TypedStateOf(Cell.serializer(), Cell.X(1))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), spec)
        }

        assertTrue(thrown.message!!.contains("tag: spec=O, impl=X"))
    }

    @Test
    fun `reports a differing sealed payload`() {
        val spec = ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("X"), "value" to ItfValue.Num(1)))
        val state = TypedStateOf(Cell.serializer(), Cell.X(2))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), spec)
        }

        assertTrue(thrown.message!!.contains("value: spec=1, impl=2"))
    }

    @Test
    fun `reports a nullable field's value against null`() {
        val spec = ItfValue.Record(
            linkedMapOf("inner" to ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("None")))),
        )
        val state = TypedStateOf(Nullable.serializer(), Nullable(5))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), spec)
        }

        assertTrue(thrown.message!!.contains("inner: spec=null, impl=5"))
    }

    @Test
    fun `reports a diff for a BigInteger field`() {
        val spec = ItfValue.Record(linkedMapOf("n" to ItfValue.BigInt("123456789012345678901234567890")))
        val state = TypedStateOf(BigCounter.serializer(), BigCounter(BigInteger("999999999999999999999999999999")))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), spec)
        }

        assertTrue(
            thrown.message!!.contains(
                "n: spec=123456789012345678901234567890, impl=999999999999999999999999999999",
            ),
        )
    }

    @Test
    fun `check prefixes a root-level decode error with state`() {
        val state = CounterState(Counter(count = 1, label = "a"))

        val thrown = assertThrows<SerializationException> {
            state.check(FakeDriver(), ItfValue.Str("not a record"))
        }

        assertTrue(thrown.message!!.startsWith("state: expected a record"))
    }

    @Test
    fun `check prefixes a nested decode error with state and keeps the original cause`() {
        val state = CounterState(Counter(count = 1, label = "a"))

        val thrown = assertThrows<SerializationException> {
            state.check(FakeDriver(), specRecord(ItfValue.Str("not a number"), "a"))
        }

        assertTrue(thrown.message!!.startsWith("state.count: expected an int"))
        assertTrue(thrown.cause is SerializationException)
        assertEquals("count: expected an int, got a string (\"not a number\")", thrown.cause!!.message)
    }

    @Test
    fun `State-disabled skips checks regardless of spec value`() {
        val state = State.disabled<FakeDriver>()

        state.check(FakeDriver(), ItfValue.Str("anything"))
        state.check(FakeDriver(), ItfValue.Record(linkedMapOf("count" to ItfValue.Num(999), "label" to ItfValue.Str("z"))))
    }
}
