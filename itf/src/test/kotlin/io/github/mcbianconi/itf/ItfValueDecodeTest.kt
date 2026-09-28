@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.itf

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger

@Serializable
private data class Counter(val count: Long, val label: String)

@Serializable
private data class HugeCount(
    val n:
    @Serializable(with = BigIntegerSerializer::class)
    BigInteger,
)

@Serializable
private data class SmallCount(
    val n:
    @Serializable(with = BigIntegerSerializer::class)
    BigInteger,
)

@Serializable
private data class ContextualHugeCount(val n: @Contextual BigInteger)

@Serializable
private data class NullableCount(val n: Long?)

@Serializable
private data class Point(val x: Long, val y: Long)

@Serializable
@JsonClassDiscriminator("tag")
private sealed class ColorSer {
    @Serializable
    @SerialName("Red")
    data object Red : ColorSer()

    @Serializable
    @SerialName("Blue")
    data object Blue : ColorSer()
}

@Serializable
private data class NullablePoint(val p: Point?)

@Serializable
@JsonClassDiscriminator("tag")
private sealed class MaybeSer {
    @Serializable
    @SerialName("Some")
    data class Some(val value: Long) : MaybeSer()

    @Serializable
    @SerialName("None")
    data object None : MaybeSer()
}

@Serializable
private data class BoardState(val board: Map<List<Long>, String>)

@Serializable
private data class Picks(val move: ColorSer)

class ItfValueDecodeTest {

    private fun point(x: Long, y: Long) =
        ItfValue.Record(linkedMapOf("x" to ItfValue.Num(x), "y" to ItfValue.Num(y)))

    private fun unitVariant(tag: String) =
        ItfValue.Record(linkedMapOf("tag" to ItfValue.Str(tag), "value" to ItfValue.Tup(emptyList())))

    @Test
    fun `decode with an explicit deserializer decodes a record into a data class`() {
        val value = ItfValue.Record(linkedMapOf("count" to ItfValue.Num(1), "label" to ItfValue.Str("a")))

        assertEquals(Counter(1, "a"), value.decode(serializer<Counter>()))
    }

    @Test
    fun `reified decode decodes a record into a data class`() {
        val value = ItfValue.Record(linkedMapOf("count" to ItfValue.Num(1), "label" to ItfValue.Str("a")))

        assertEquals(Counter(1, "a"), value.decode<Counter>())
    }

    @Test
    fun `decode unwraps a Some option field into the non-null value`() {
        val value = ItfValue.Record(
            linkedMapOf(
                "n" to ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("Some"), "value" to ItfValue.Num(7))),
            ),
        )

        assertEquals(NullableCount(7L), value.decode<NullableCount>())
    }

    @Test
    fun `decode unwraps a None option field into null`() {
        val value = ItfValue.Record(
            linkedMapOf("n" to ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("None")))),
        )

        assertNull(value.decode<NullableCount>().n)
    }

    @Test
    fun `decode unwraps a Some option field nested inside a record`() {
        val value = ItfValue.Record(
            linkedMapOf("p" to ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("Some"), "value" to point(1, 2)))),
        )

        assertEquals(NullablePoint(Point(1, 2)), value.decode<NullablePoint>())
    }

    @Test
    fun `non-nullable sum type with Some or None variant names decodes as-is`() {
        val none = ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("None")))
        val some = ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("Some"), "value" to ItfValue.Num(7)))

        assertEquals(MaybeSer.None, none.decode<MaybeSer>())
        assertEquals(MaybeSer.Some(7), some.decode<MaybeSer>())
    }

    @Test
    fun `decode a BigInteger field beyond Long range`() {
        val huge = "123456789012345678901234567890"
        val value = ItfValue.Record(linkedMapOf("n" to ItfValue.BigInt(huge)))

        assertEquals(HugeCount(BigInteger(huge)), value.decode<HugeCount>())
    }

    @Test
    fun `decode a BigInteger field that fits in a Long`() {
        val value = ItfValue.Record(linkedMapOf("n" to ItfValue.BigInt("42")))

        assertEquals(HugeCount(BigInteger.valueOf(42)), value.decode<HugeCount>())
    }

    @Test
    fun `decode a Contextual BigInteger field beyond Long range`() {
        val huge = "123456789012345678901234567890"
        val value = ItfValue.Record(linkedMapOf("n" to ItfValue.BigInt(huge)))

        assertEquals(ContextualHugeCount(BigInteger(huge)), value.decode<ContextualHugeCount>())
    }

    @Test
    fun `decode a plain Long field from a BigInt value that fits`() {
        val value = ItfValue.Record(linkedMapOf("n" to ItfValue.BigInt("42")))

        assertEquals(SmallCount(BigInteger.valueOf(42)), value.decode<SmallCount>())
    }

    @Test
    fun `set of primitives decodes into Set regardless of element order`() {
        val setA = ItfValue.Set(listOf(ItfValue.Num(1), ItfValue.Num(2), ItfValue.Num(3)))
        val setB = ItfValue.Set(listOf(ItfValue.Num(3), ItfValue.Num(1), ItfValue.Num(2)))

        assertEquals(setOf(1L, 2L, 3L), setA.decode<Set<Long>>())
        assertEquals(setA.decode<Set<Long>>(), setB.decode<Set<Long>>())
    }

    @Test
    fun `set of records decodes into Set regardless of element order`() {
        val setA = ItfValue.Set(listOf(point(1, 2), point(3, 4)))
        val setB = ItfValue.Set(listOf(point(3, 4), point(1, 2)))

        assertEquals(setOf(Point(1, 2), Point(3, 4)), setA.decode<Set<Point>>())
        assertEquals(setA.decode<Set<Point>>(), setB.decode<Set<Point>>())
    }

    @Test
    fun `set of sum type with unit variant decodes regardless of element order`() {
        val setA = ItfValue.Set(listOf(unitVariant("Red"), unitVariant("Blue")))
        val setB = ItfValue.Set(listOf(unitVariant("Blue"), unitVariant("Red")))

        assertEquals(setOf(ColorSer.Red, ColorSer.Blue), setA.decode<Set<ColorSer>>())
        assertEquals(setA.decode<Set<ColorSer>>(), setB.decode<Set<ColorSer>>())
    }

    @Test
    fun `map with string keys decodes into Map`() {
        val map = ItfValue.Map(listOf(ItfValue.Str("a") to ItfValue.Num(1)))

        assertEquals(mapOf("a" to 1L), map.decode<Map<String, Long>>())
    }

    @Test
    fun `map with int keys decodes into Map`() {
        val map = ItfValue.Map(listOf(ItfValue.Num(1) to ItfValue.Str("x")))

        assertEquals(mapOf(1L to "x"), map.decode<Map<Long, String>>())
    }

    @Test
    fun `map with bool keys decodes into Map`() {
        val map = ItfValue.Map(listOf(ItfValue.Bool(true) to ItfValue.Num(1)))

        assertEquals(mapOf(true to 1L), map.decode<Map<Boolean, Long>>())
    }

    @Test
    fun `map with tuple keys decodes into Map of List`() {
        val map = ItfValue.Map(
            listOf(
                ItfValue.Tup(listOf(ItfValue.Num(1), ItfValue.Num(2))) to ItfValue.Str("a"),
                ItfValue.Tup(listOf(ItfValue.Num(3), ItfValue.Num(4))) to ItfValue.Str("b"),
            ),
        )

        assertEquals(mapOf(listOf(1L, 2L) to "a", listOf(3L, 4L) to "b"), map.decode<Map<List<Long>, String>>())
    }

    @Test
    fun `map with tuple keys does not decode into Map of Pair`() {
        val map = ItfValue.Map(listOf(ItfValue.Tup(listOf(ItfValue.Num(1), ItfValue.Num(2))) to ItfValue.Str("a")))

        assertThrows<SerializationException> {
            map.decode<Map<Pair<Long, Long>, String>>()
        }
    }

    @Test
    fun `map with record keys decodes into Map of data class`() {
        val map = ItfValue.Map(
            listOf(
                point(1, 2) to ItfValue.Str("near"),
                point(3, 4) to ItfValue.Str("far"),
            ),
        )

        assertEquals(mapOf(Point(1, 2) to "near", Point(3, 4) to "far"), map.decode<Map<Point, String>>())
    }

    @Test
    fun `map with sum type keys decodes into Map of sealed class`() {
        val map = ItfValue.Map(
            listOf(
                unitVariant("Red") to ItfValue.Num(1),
                unitVariant("Blue") to ItfValue.Num(2),
            ),
        )

        assertEquals(mapOf(ColorSer.Red to 1L, ColorSer.Blue to 2L), map.decode<Map<ColorSer, Long>>())
    }

    @Test
    fun `empty map decodes regardless of key type`() {
        val empty = ItfValue.Map(emptyList())

        assertEquals(emptyMap<Long, String>(), empty.decode<Map<Long, String>>())
        assertEquals(emptyMap<List<Long>, String>(), empty.decode<Map<List<Long>, String>>())
        assertEquals(emptyMap<Point, String>(), empty.decode<Map<Point, String>>())
    }

    @Test
    fun `wrong kind at a nested map value names the field path and both kinds`() {
        val value = ItfValue.Record(
            linkedMapOf(
                "board" to ItfValue.Map(
                    listOf(ItfValue.Tup(listOf(ItfValue.Num(1), ItfValue.Num(2))) to ItfValue.Num(5)),
                ),
            ),
        )

        val exception = assertThrows<ItfDecodingException> { value.decode<BoardState>() }
        assertTrue(exception.message!!.contains("board.(1, 2)"), exception.message)
        assertTrue(exception.message!!.contains("expected a string"), exception.message)
        assertTrue(exception.message!!.contains("an int"), exception.message)
    }

    @Test
    fun `missing tag field names the field path`() {
        val value = ItfValue.Record(linkedMapOf("move" to ItfValue.Record(linkedMapOf("other" to ItfValue.Num(1)))))

        val exception = assertThrows<ItfDecodingException> { value.decode<Picks>() }
        assertTrue(exception.message!!.contains("move.tag"), exception.message)
    }

    @Test
    fun `unknown tag value names the field path and the valid variants`() {
        val value = ItfValue.Record(
            linkedMapOf("move" to ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("Purple"), "value" to ItfValue.Tup(emptyList())))),
        )

        val exception = assertThrows<ItfDecodingException> { value.decode<Picks>() }
        assertTrue(exception.message!!.contains("move.tag"), exception.message)
        assertTrue(exception.message!!.contains("Red"), exception.message)
        assertTrue(exception.message!!.contains("Blue"), exception.message)
    }
}
