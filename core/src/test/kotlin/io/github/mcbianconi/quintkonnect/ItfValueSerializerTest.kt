package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.itf.ItfValue
import io.github.mcbianconi.quintkonnect.itf.ItfValueSerializer
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ItfValueSerializerTest {

    private fun parse(json: String): ItfValue =
        ItfValueSerializer.fromJsonElement(Json.parseToJsonElement(json))

    @Test
    fun `deserialize bool true`() = assertEquals(ItfValue.Bool(true), parse("true"))

    @Test
    fun `deserialize bool false`() = assertEquals(ItfValue.Bool(false), parse("false"))

    @Test
    fun `deserialize number`() = assertEquals(ItfValue.Num(42), parse("42"))

    @Test
    fun `deserialize string`() = assertEquals(ItfValue.Str("hello"), parse("\"hello\""))

    @Test
    fun `deserialize bigint`() = assertEquals(ItfValue.BigInt("12345"), parse("{\"#bigint\": \"12345\"}"))

    @Test
    fun `deserialize list`() =
        assertEquals(
            ItfValue.List(listOf(ItfValue.Num(1), ItfValue.Num(2))),
            parse("[1, 2]"),
        )

    @Test
    fun `deserialize tuple`() =
        assertEquals(
            ItfValue.Tup(listOf(ItfValue.Num(1), ItfValue.Bool(true))),
            parse("{\"#tup\": [1, true]}"),
        )

    @Test
    fun `deserialize set`() =
        assertEquals(
            ItfValue.Set(listOf(ItfValue.Num(1), ItfValue.Num(2))),
            parse("{\"#set\": [1, 2]}"),
        )

    @Test
    fun `deserialize map`() =
        assertEquals(
            ItfValue.Map(listOf(ItfValue.Str("a") to ItfValue.Num(1))),
            parse("{\"#map\": [[\"a\", 1]]}"),
        )

    @Test
    fun `deserialize record strips meta`() {
        val result = parse("{\"#meta\": {}, \"x\": 1, \"y\": 2}")
        val expected = ItfValue.Record(linkedMapOf("x" to ItfValue.Num(1), "y" to ItfValue.Num(2)))
        assertEquals(expected, result)
    }

    @Test
    fun `round-trip bool`() {
        val v = ItfValue.Bool(true)
        assertEquals(v, ItfValueSerializer.fromJsonElement(ItfValueSerializer.toJsonElement(v)))
    }

    @Test
    fun `round-trip bigint`() {
        val v = ItfValue.BigInt("999")
        assertEquals(v, ItfValueSerializer.fromJsonElement(ItfValueSerializer.toJsonElement(v)))
    }

    @Test
    fun `round-trip record`() {
        val v = ItfValue.Record(linkedMapOf("a" to ItfValue.Num(1), "b" to ItfValue.Bool(false)))
        assertEquals(v, ItfValueSerializer.fromJsonElement(ItfValueSerializer.toJsonElement(v)))
    }
}
