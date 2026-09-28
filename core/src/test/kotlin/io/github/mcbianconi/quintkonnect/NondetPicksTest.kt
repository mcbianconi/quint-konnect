package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.quintkonnect.nondet.NondetPicks
import io.github.mcbianconi.quintkonnect.nondet.decode
import io.github.mcbianconi.quintkonnect.nondet.decodeOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Serializable
data class NondetHolder(val inner: Long?)

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
            ),
        )
        val picks = NondetPicks.fromItfValue(
            ItfValue.Record(linkedMapOf("foo" to option)),
        )
        assertEquals(ItfValue.Num(42), picks.get("foo"))
    }

    @Test
    fun `fromItfValue drops None option`() {
        val none = ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("None")))
        val picks = NondetPicks.fromItfValue(
            ItfValue.Record(linkedMapOf("foo" to none)),
        )
        assertNull(picks.get("foo"))
    }

    @Test
    fun `decode extracts typed value`() {
        val picks = NondetPicks.fromItfValue(
            ItfValue.Record(linkedMapOf("n" to ItfValue.Num(7))),
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
            ItfValue.Record(linkedMapOf("n" to ItfValue.Num(3))),
        )
        assertEquals(3L, picks.decodeOrNull<Long>("n"))
    }

    @Test
    fun `decode unwraps a nullable field nested inside a pick's value`() {
        val innerSome = ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("Some"), "value" to ItfValue.Num(5)))
        val picks = NondetPicks.fromItfValue(
            ItfValue.Record(linkedMapOf("holder" to ItfValue.Record(linkedMapOf("inner" to innerSome)))),
        )
        assertEquals(NondetHolder(5L), picks.decode<NondetHolder>("holder"))
    }

    @Test
    fun `decode prefixes a root-level decode error with picks-name`() {
        val picks = NondetPicks.fromItfValue(
            ItfValue.Record(linkedMapOf("n" to ItfValue.Str("not a number"))),
        )

        val thrown = assertThrows<SerializationException> { picks.decode<Long>("n") }

        assertTrue(thrown.message!!.startsWith("picks.n: expected an int"))
    }

    @Test
    fun `decode prefixes a nested decode error with picks-name and keeps the original cause`() {
        val picks = NondetPicks.fromItfValue(
            ItfValue.Record(
                linkedMapOf(
                    "holder" to ItfValue.Record(linkedMapOf("inner" to ItfValue.Str("not a number"))),
                ),
            ),
        )

        val thrown = assertThrows<SerializationException> { picks.decode<NondetHolder>("holder") }

        assertTrue(thrown.message!!.startsWith("picks.holder.inner: expected an int"))
        assertTrue(thrown.cause is SerializationException)
        assertEquals("inner: expected an int, got a string (\"not a number\")", thrown.cause!!.message)
    }

    @Test
    fun `decodeOrNull prefixes a decode error with picks-name`() {
        val picks = NondetPicks.fromItfValue(
            ItfValue.Record(linkedMapOf("n" to ItfValue.Str("not a number"))),
        )

        val thrown = assertThrows<SerializationException> { picks.decodeOrNull<Long>("n") }

        assertTrue(thrown.message!!.startsWith("picks.n: expected an int"))
    }
}
