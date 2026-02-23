package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.itf.ItfValue
import io.github.mcbianconi.quintkonnect.nondet.NondetPicks
import io.github.mcbianconi.quintkonnect.nondet.decode
import io.github.mcbianconi.quintkonnect.nondet.decodeOrNull
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class NondetPicksTest {

    @Test
    fun `fromItfValue rejects non-record`() {
        assertThrows<IllegalArgumentException> {
            NondetPicks.fromItfValue(ItfValue.Num(42))
        }
    }

    @Test
    fun `fromItfValue unwraps Some option`() {
        val option = ItfValue.Record(
            linkedMapOf(
                "tag" to ItfValue.Str("Some"),
                "value" to ItfValue.Num(42),
            )
        )
        val picks = NondetPicks.fromItfValue(
            ItfValue.Record(linkedMapOf("foo" to option))
        )
        assertEquals(ItfValue.Num(42), picks.get("foo"))
    }

    @Test
    fun `fromItfValue drops None option`() {
        val none = ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("None")))
        val picks = NondetPicks.fromItfValue(
            ItfValue.Record(linkedMapOf("foo" to none))
        )
        assertNull(picks.get("foo"))
    }

    @Test
    fun `decode extracts typed value`() {
        val picks = NondetPicks.fromItfValue(
            ItfValue.Record(linkedMapOf("n" to ItfValue.Num(7)))
        )
        assertEquals(7L, picks.decode<Long>("n"))
    }

    @Test
    fun `decode throws when pick is missing`() {
        val picks = NondetPicks.empty()
        assertThrows<IllegalStateException> { picks.decode<Long>("n") }
    }

    @Test
    fun `decodeOrNull returns null when missing`() {
        assertNull(NondetPicks.empty().decodeOrNull<Long>("n"))
    }

    @Test
    fun `decodeOrNull returns value when present`() {
        val picks = NondetPicks.fromItfValue(
            ItfValue.Record(linkedMapOf("n" to ItfValue.Num(3)))
        )
        assertEquals(3L, picks.decodeOrNull<Long>("n"))
    }
}
