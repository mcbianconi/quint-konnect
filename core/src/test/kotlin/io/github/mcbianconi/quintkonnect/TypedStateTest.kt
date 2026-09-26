package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.itf.ItfValue
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Serializable
private data class Counter(val count: Long, val label: String)

class TypedStateTest {

    private class FakeDriver : Driver {
        override fun step(step: Step) {}
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
    fun `throws with a diff naming the differing field on a mismatch`() {
        val state = CounterState(Counter(count = 2, label = "a"))

        val thrown = assertThrows<IllegalStateException> {
            state.check(FakeDriver(), specRecord(ItfValue.Num(1), "a"))
        }

        assertTrue(thrown.message!!.contains("-Counter(count=1, label=a)"))
        assertTrue(thrown.message!!.contains("+Counter(count=2, label=a)"))
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

        assertTrue(thrown.message!!.contains("-Counter(count=6, label=x)"))
        assertTrue(thrown.message!!.contains("+Counter(count=5, label=x)"))
    }

    @Test
    fun `State-disabled skips checks regardless of spec value`() {
        val state = State.disabled<FakeDriver>()

        state.check(FakeDriver(), ItfValue.Str("anything"))
        state.check(FakeDriver(), ItfValue.Record(linkedMapOf("count" to ItfValue.Num(999), "label" to ItfValue.Str("z"))))
    }
}
