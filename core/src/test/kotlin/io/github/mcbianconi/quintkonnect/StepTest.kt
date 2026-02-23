package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.itf.ItfValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StepTest {

    private val emptyNondet = ItfValue.Record(LinkedHashMap())

    private fun state(vararg pairs: Pair<String, ItfValue>): LinkedHashMap<String, ItfValue> =
        linkedMapOf(*pairs)

    @Test
    fun `extracts action from mbt vars`() {
        val state = state(
            "mbt::actionTaken" to ItfValue.Str("TestAction"),
            "mbt::nondetPicks" to emptyNondet,
            "x" to ItfValue.Num(1),
        )
        val step = Step.fromState(state, DriverConfig())
        assertEquals("TestAction", step.actionTaken)
    }

    @Test
    fun `extracts state value at path`() {
        val nested = ItfValue.Record(linkedMapOf("inner" to ItfValue.Num(99)))
        val state = state(
            "mbt::actionTaken" to ItfValue.Str("A"),
            "mbt::nondetPicks" to emptyNondet,
            "outer" to nested,
        )
        val step = Step.fromState(state, DriverConfig(statePath = listOf("outer", "inner")))
        assertEquals(ItfValue.Num(99), step.state)
    }

    @Test
    fun `extracts action from sum type`() {
        val sumRecord = ItfValue.Record(
            linkedMapOf(
                "tag" to ItfValue.Str("SumAction"),
                "value" to ItfValue.Tup(emptyList()),
            )
        )
        val state = state("action" to sumRecord, "x" to ItfValue.Num(5))
        val step = Step.fromState(state, DriverConfig(nondetPath = listOf("action")))
        assertEquals("SumAction", step.actionTaken)
        assertTrue(step.nondetPicks.isEmpty())
    }

    @Test
    fun `sum type extraction strips mbt vars`() {
        val sumRecord = ItfValue.Record(
            linkedMapOf(
                "tag" to ItfValue.Str("A"),
                "value" to ItfValue.Tup(emptyList()),
            )
        )
        val state = state(
            "action" to sumRecord,
            "mbt::actionTaken" to ItfValue.Str("old"),
            "mbt::nondetPicks" to emptyNondet,
        )
        val step = Step.fromState(state, DriverConfig(nondetPath = listOf("action")))
        assertEquals("A", step.actionTaken)
    }

    @Test
    fun `missing mbt actionTaken throws`() {
        val state = state("mbt::nondetPicks" to emptyNondet)
        assertThrows<IllegalStateException> { Step.fromState(state, DriverConfig()) }
    }

    @Test
    fun `missing mbt nondetPicks throws`() {
        val state = state("mbt::actionTaken" to ItfValue.Str("A"))
        assertThrows<IllegalStateException> { Step.fromState(state, DriverConfig()) }
    }
}
