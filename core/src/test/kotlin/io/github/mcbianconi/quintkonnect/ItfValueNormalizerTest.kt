@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.itf.ItfValue
import io.github.mcbianconi.quintkonnect.itf.QuintJson
import io.github.mcbianconi.quintkonnect.itf.toNormalizedJson
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Serializable
data class Point(val x: Long, val y: Long)

@Serializable
@JsonClassDiscriminator("tag")
sealed class ColorSer {
    @Serializable @SerialName("Red") data object Red : ColorSer()
    @Serializable @SerialName("Blue") data object Blue : ColorSer()
}

class ItfValueNormalizerTest {

    private fun point(x: Long, y: Long) =
        ItfValue.Record(linkedMapOf("x" to ItfValue.Num(x), "y" to ItfValue.Num(y)))

    private fun unitVariant(tag: String) =
        ItfValue.Record(linkedMapOf("tag" to ItfValue.Str(tag), "value" to ItfValue.Tup(emptyList())))

    @Test
    fun `set of primitives decodes into Set regardless of element order`() {
        val setA = ItfValue.Set(listOf(ItfValue.Num(1), ItfValue.Num(2), ItfValue.Num(3)))
        val setB = ItfValue.Set(listOf(ItfValue.Num(3), ItfValue.Num(1), ItfValue.Num(2)))

        val decodedA = QuintJson.decodeFromJsonElement<Set<Long>>(setA.toNormalizedJson())
        val decodedB = QuintJson.decodeFromJsonElement<Set<Long>>(setB.toNormalizedJson())

        assertEquals(setOf(1L, 2L, 3L), decodedA)
        assertEquals(decodedA, decodedB)
    }

    @Test
    fun `set of records decodes into Set regardless of element order`() {
        val setA = ItfValue.Set(listOf(point(1, 2), point(3, 4)))
        val setB = ItfValue.Set(listOf(point(3, 4), point(1, 2)))

        val decodedA = QuintJson.decodeFromJsonElement<Set<Point>>(setA.toNormalizedJson())
        val decodedB = QuintJson.decodeFromJsonElement<Set<Point>>(setB.toNormalizedJson())

        assertEquals(setOf(Point(1, 2), Point(3, 4)), decodedA)
        assertEquals(decodedA, decodedB)
    }

    @Test
    fun `set of sum type with unit variant decodes regardless of element order`() {
        val setA = ItfValue.Set(listOf(unitVariant("Red"), unitVariant("Blue")))
        val setB = ItfValue.Set(listOf(unitVariant("Blue"), unitVariant("Red")))

        val decodedA = QuintJson.decodeFromJsonElement<Set<ColorSer>>(setA.toNormalizedJson())
        val decodedB = QuintJson.decodeFromJsonElement<Set<ColorSer>>(setB.toNormalizedJson())

        assertEquals(setOf(ColorSer.Red, ColorSer.Blue), decodedA)
        assertEquals(decodedA, decodedB)
    }

    @Test
    fun `map with string keys normalizes to JsonObject and decodes as before`() {
        val map = ItfValue.Map(listOf(ItfValue.Str("a") to ItfValue.Num(1)))

        val json = map.toNormalizedJson()
        assertTrue(json is JsonObject)
        assertEquals(mapOf("a" to 1L), QuintJson.decodeFromJsonElement<Map<String, Long>>(json))
    }

    @Test
    fun `map with int keys normalizes to JsonObject and decodes as before`() {
        val map = ItfValue.Map(listOf(ItfValue.Num(1) to ItfValue.Str("x")))

        val json = map.toNormalizedJson()
        assertTrue(json is JsonObject)
        assertEquals(mapOf(1L to "x"), QuintJson.decodeFromJsonElement<Map<Long, String>>(json))
    }

    @Test
    fun `map with bool keys normalizes to JsonObject and decodes as before`() {
        val map = ItfValue.Map(listOf(ItfValue.Bool(true) to ItfValue.Num(1)))

        val json = map.toNormalizedJson()
        assertTrue(json is JsonObject)
        assertEquals(mapOf(true to 1L), QuintJson.decodeFromJsonElement<Map<Boolean, Long>>(json))
    }

    @Test
    fun `map with tuple keys normalizes to flat JsonArray and decodes into Map of List`() {
        val map = ItfValue.Map(
            listOf(
                ItfValue.Tup(listOf(ItfValue.Num(1), ItfValue.Num(2))) to ItfValue.Str("a"),
                ItfValue.Tup(listOf(ItfValue.Num(3), ItfValue.Num(4))) to ItfValue.Str("b"),
            )
        )

        val json = map.toNormalizedJson()
        assertTrue(json is JsonArray)

        val decoded = QuintJson.decodeFromJsonElement<Map<List<Long>, String>>(json)
        assertEquals(mapOf(listOf(1L, 2L) to "a", listOf(3L, 4L) to "b"), decoded)
    }

    @Test
    fun `map with tuple keys does not decode into Map of Pair`() {
        val map = ItfValue.Map(listOf(ItfValue.Tup(listOf(ItfValue.Num(1), ItfValue.Num(2))) to ItfValue.Str("a")))
        val json = map.toNormalizedJson()

        assertThrows<SerializationException> {
            QuintJson.decodeFromJsonElement<Map<Pair<Long, Long>, String>>(json)
        }
    }

    @Test
    fun `map with record keys normalizes to flat JsonArray and decodes into Map of data class`() {
        val map = ItfValue.Map(
            listOf(
                point(1, 2) to ItfValue.Str("near"),
                point(3, 4) to ItfValue.Str("far"),
            )
        )

        val json = map.toNormalizedJson()
        assertTrue(json is JsonArray)

        val decoded = QuintJson.decodeFromJsonElement<Map<Point, String>>(json)
        assertEquals(mapOf(Point(1, 2) to "near", Point(3, 4) to "far"), decoded)
    }

    @Test
    fun `map with sum type keys normalizes to flat JsonArray and decodes into Map of sealed class`() {
        val map = ItfValue.Map(
            listOf(
                unitVariant("Red") to ItfValue.Num(1),
                unitVariant("Blue") to ItfValue.Num(2),
            )
        )

        val json = map.toNormalizedJson()
        assertTrue(json is JsonArray)

        val decoded = QuintJson.decodeFromJsonElement<Map<ColorSer, Long>>(json)
        assertEquals(mapOf(ColorSer.Red to 1L, ColorSer.Blue to 2L), decoded)
    }

    @Test
    fun `empty map normalizes to JsonObject when no descriptor is given`() {
        // Without a descriptor, an empty ItfValue.Map carries no entries to tell an intended
        // tuple/record key type apart from a primitive one, so it always produces `{}`. That only
        // decodes into primitive-keyed Kotlin maps (Map<Long, V>, Map<String, V>, ...); an empty
        // Map<List<Long>, V> or Map<R, V> fails to decode.
        val json = ItfValue.Map(emptyList()).toNormalizedJson()

        assertEquals(JsonObject(emptyMap()), json)
        assertEquals(emptyMap<Long, String>(), QuintJson.decodeFromJsonElement<Map<Long, String>>(json))
        assertThrows<SerializationException> {
            QuintJson.decodeFromJsonElement<Map<List<Long>, String>>(json)
        }
    }

    @Test
    fun `empty map with tuple-key descriptor normalizes to JsonArray and decodes`() {
        val descriptor = serializer<Map<List<Long>, String>>().descriptor
        val json = ItfValue.Map(emptyList()).toNormalizedJson(descriptor)

        assertTrue(json is JsonArray)
        assertEquals(emptyMap<List<Long>, String>(), QuintJson.decodeFromJsonElement<Map<List<Long>, String>>(json))
    }

    @Test
    fun `empty map with record-key descriptor normalizes to JsonArray and decodes`() {
        val descriptor = serializer<Map<Point, String>>().descriptor
        val json = ItfValue.Map(emptyList()).toNormalizedJson(descriptor)

        assertTrue(json is JsonArray)
        assertEquals(emptyMap<Point, String>(), QuintJson.decodeFromJsonElement<Map<Point, String>>(json))
    }

    @Test
    fun `empty map with primitive-key descriptor still normalizes to JsonObject`() {
        val descriptor = serializer<Map<Long, String>>().descriptor
        val json = ItfValue.Map(emptyList()).toNormalizedJson(descriptor)

        assertEquals(JsonObject(emptyMap()), json)
    }
}
