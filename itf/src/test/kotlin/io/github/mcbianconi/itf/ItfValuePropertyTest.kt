@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.itf

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.random.Random

@Serializable
private data class PPoint(val x: Long, val y: Long)

@Serializable
@JsonClassDiscriminator("tag")
private sealed class PShape {
    @Serializable @SerialName("Circle") data class Circle(val value: Long) : PShape()
    @Serializable @SerialName("Square") data object Square : PShape()
}

@Serializable
private data class PState(
    val n: Long,
    val flag: Boolean,
    val label: String,
    val big: @Contextual BigInteger,
    val xs: List<Long>,
    val tags: Set<String>,
    val points: Set<PPoint>,
    val counts: Map<Long, String>,
    val board: Map<List<Long>, PShape>,
    val maybeN: Long?,
    val maybePoint: PPoint?,
    val shapes: List<PShape>,
)

/**
 * Round-trip property tests for [ItfValueSerializer] and [ItfValue.decode].
 *
 * No external property-testing library: each generator below is a small seeded function over
 * [kotlin.random.Random], run for a fixed set of seeds and a fixed case count per seed (shrinking
 * not implemented). A failure's assertion message carries the seed, case index and [ItfValue.display]
 * so a specific failing case can be pinned down and turned into its own regular test.
 */
class ItfValuePropertyTest {

    private val safeFieldNames = listOf("f0", "f1", "f2", "label", "key", "child", "note")

    private fun genRawString(random: Random): String = when (random.nextInt(6)) {
        0 -> ""
        1 -> "true"
        2 -> "42"
        3 -> "with \"quotes\", \\backslash\\ and \nnewline"
        4 -> "unicode: éè中文😀"
        else -> (0 until random.nextInt(12)).map { ('a' + random.nextInt(26)) }.joinToString("")
    }

    private fun genEdgeLong(random: Random): Long = when (random.nextInt(6)) {
        0 -> 0L
        1 -> 1L
        2 -> -1L
        3 -> Long.MAX_VALUE
        4 -> Long.MIN_VALUE
        else -> random.nextLong()
    }

    private fun genBigIntString(random: Random): String {
        val magnitude = BigInteger(96 + random.nextInt(64), java.util.Random(random.nextLong()))
        val signed = if (random.nextBoolean()) magnitude.negate() else magnitude
        return signed.toString()
    }

    /** Generic [ItfValue] tree for the [ItfValueSerializer] round-trip property; depth-bounded. */
    private fun genItfValue(random: Random, depth: Int): ItfValue {
        val choiceBound = if (depth <= 0) 4 else 8
        return when (random.nextInt(choiceBound)) {
            0 -> ItfValue.Bool(random.nextBoolean())
            1 -> ItfValue.Num(genEdgeLong(random))
            2 -> ItfValue.Str(genRawString(random))
            3 -> ItfValue.BigInt(genBigIntString(random))
            4 -> ItfValue.List(genItfChildren(random, depth))
            5 -> ItfValue.Tup(genItfChildren(random, depth))
            6 -> ItfValue.Set(genItfChildren(random, depth))
            else -> if (random.nextBoolean()) {
                ItfValue.Map(genItfChildren(random, depth).map { genItfValue(random, depth - 1) to it })
            } else {
                val fields = LinkedHashMap<String, ItfValue>()
                repeat(random.nextInt(4)) { i ->
                    fields[safeFieldNames[i % safeFieldNames.size] + i] = genItfValue(random, depth - 1)
                }
                ItfValue.Record(fields)
            }
        }
    }

    private fun genItfChildren(random: Random, depth: Int): List<ItfValue> =
        (0 until random.nextInt(4)).map { genItfValue(random, depth - 1) }

    @Test
    fun `ItfValueSerializer round-trips arbitrary values through JSON`() {
        for (seed in listOf(7L, 99L, 2026L)) {
            val random = Random(seed)
            repeat(200) { i ->
                val value = genItfValue(random, depth = 3)
                val json = Json.encodeToString(ItfValueSerializer, value)
                val roundTripped = Json.decodeFromString(ItfValueSerializer, json)
                assertEquals(value, roundTripped, "seed=$seed case=$i json=$json")
            }
        }
    }

    // --- ItfValue.decode property: a hand-built Kotlin value paired with the ITF encoding Quint
    // would emit for it (never derived from the encoder/decoder under test). ---

    private fun genLong(random: Random): Pair<Long, ItfValue> {
        val v = genEdgeLong(random)
        val itf = if (random.nextBoolean()) ItfValue.Num(v) else ItfValue.BigInt(v.toString())
        return v to itf
    }

    private fun genBigInteger(random: Random): Pair<BigInteger, ItfValue> {
        val huge = random.nextBoolean()
        val v = if (huge) BigInteger(genBigIntString(random)) else BigInteger.valueOf(genEdgeLong(random))
        val itf = if (!huge && random.nextBoolean()) ItfValue.Num(v.toLong()) else ItfValue.BigInt(v.toString())
        return v to itf
    }

    private fun genString(random: Random): Pair<String, ItfValue> {
        val v = genRawString(random)
        return v to ItfValue.Str(v)
    }

    private fun genPoint(random: Random): Pair<PPoint, ItfValue> {
        val x = genEdgeLong(random)
        val y = genEdgeLong(random)
        return PPoint(x, y) to ItfValue.Record(linkedMapOf("x" to ItfValue.Num(x), "y" to ItfValue.Num(y)))
    }

    private fun sumVariant(tag: String, value: ItfValue): ItfValue =
        ItfValue.Record(linkedMapOf("tag" to ItfValue.Str(tag), "value" to value))

    private fun genShape(random: Random): Pair<PShape, ItfValue> = if (random.nextBoolean()) {
        val (value, valueItf) = genLong(random)
        PShape.Circle(value) to sumVariant("Circle", valueItf)
    } else {
        PShape.Square to sumVariant("Square", ItfValue.Tup(emptyList()))
    }

    private fun <V> genOption(
        random: Random,
        genValue: (Random) -> Pair<V, ItfValue>,
    ): Pair<V?, ItfValue> = if (random.nextBoolean()) {
        val (v, itf) = genValue(random)
        v to ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("Some"), "value" to itf))
    } else {
        null to ItfValue.Record(linkedMapOf("tag" to ItfValue.Str("None")))
    }

    private fun <V> genList(random: Random, genValue: (Random) -> Pair<V, ItfValue>): Pair<List<V>, ItfValue> {
        val pairs = (0 until random.nextInt(5)).map { genValue(random) }
        return pairs.map { it.first } to ItfValue.List(pairs.map { it.second })
    }

    private fun <V> genSet(random: Random, genValue: (Random) -> Pair<V, ItfValue>): Pair<Set<V>, ItfValue> {
        val pairs = (0 until random.nextInt(4)).map { genValue(random) }.distinctBy { it.first }
        val shuffled = pairs.map { it.second }.shuffled(java.util.Random(random.nextLong()))
        return pairs.map { it.first }.toSet() to ItfValue.Set(shuffled)
    }

    private fun genLongKeyedMap(random: Random): Pair<Map<Long, String>, ItfValue> {
        val n = random.nextInt(4)
        val base = random.nextLong()
        val keys = (0 until n).map { base + it }
        val values = (0 until n).map { genString(random) }
        val itfEntries = keys.zip(values)
            .map { (k, v) -> ItfValue.Num(k) as ItfValue to v.second }
            .shuffled(java.util.Random(random.nextLong()))
        return keys.zip(values.map { it.first }).toMap() to ItfValue.Map(itfEntries)
    }

    private fun genTupleKeyedMap(random: Random): Pair<Map<List<Long>, PShape>, ItfValue> {
        val n = random.nextInt(4)
        val baseX = random.nextLong()
        val baseY = random.nextLong()
        val keys = (0 until n).map { listOf(baseX + it, baseY + it) }
        val values = (0 until n).map { genShape(random) }
        val itfEntries = keys.zip(values).map { (k, v) ->
            ItfValue.Tup(k.map { ItfValue.Num(it) }) as ItfValue to v.second
        }.shuffled(java.util.Random(random.nextLong()))
        return keys.zip(values.map { it.first }).toMap() to ItfValue.Map(itfEntries)
    }

    private fun genPState(random: Random): Pair<PState, ItfValue> {
        val (n, nItf) = genLong(random)
        val flag = random.nextBoolean()
        val (label, labelItf) = genString(random)
        val (big, bigItf) = genBigInteger(random)
        val (xs, xsItf) = genList(random, ::genLong)
        val (tags, tagsItf) = genSet(random, ::genString)
        val (points, pointsItf) = genSet(random, ::genPoint)
        val (counts, countsItf) = genLongKeyedMap(random)
        val (board, boardItf) = genTupleKeyedMap(random)
        val (maybeN, maybeNItf) = genOption(random, ::genLong)
        val (maybePoint, maybePointItf) = genOption(random, ::genPoint)
        val (shapes, shapesItf) = genList(random, ::genShape)

        val kotlinValue = PState(n, flag, label, big, xs, tags, points, counts, board, maybeN, maybePoint, shapes)
        val itfValue = ItfValue.Record(
            linkedMapOf(
                "n" to nItf,
                "flag" to ItfValue.Bool(flag),
                "label" to labelItf,
                "big" to bigItf,
                "xs" to xsItf,
                "tags" to tagsItf,
                "points" to pointsItf,
                "counts" to countsItf,
                "board" to boardItf,
                "maybeN" to maybeNItf,
                "maybePoint" to maybePointItf,
                "shapes" to shapesItf,
            ),
        )
        return kotlinValue to itfValue
    }

    @Test
    fun `ItfValue decode matches a hand-built ITF encoding across many random PState values`() {
        for (seed in listOf(1L, 42L, 12345L)) {
            val random = Random(seed)
            repeat(200) { i ->
                val (expected, itf) = genPState(random)
                val decoded = itf.decode<PState>()
                assertEquals(expected, decoded, "seed=$seed case=$i itf=${itf.display()}")
            }
        }
    }

    @Test
    @Disabled(
        "qk-uhta: ItfValueSerializer.fromObject doesn't special-case ADR-015's " +
            "{\"#unserializable\": \"...\"} shape, so it decodes to a Record with a literal " +
            "\"#unserializable\" field instead of ItfValue.Unserializable",
    )
    fun `ItfValueSerializer parses ADR-015 unserializable values`() {
        val parsed = Json.decodeFromString(ItfValueSerializer, """{"#unserializable": "Int"}""")

        assertEquals(ItfValue.Unserializable("Int"), parsed)
    }
}
