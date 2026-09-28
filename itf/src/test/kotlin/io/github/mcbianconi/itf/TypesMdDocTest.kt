@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.itf

import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.io.File
import java.math.BigInteger

@Serializable private data class IntRow(val v: Long)

@Serializable private data class BigNumRow(val v: @Contextual BigInteger)

@Serializable private data class BoolRow(val v: Boolean)

@Serializable private data class StrRow(val v: String)

@Serializable private data class TupleRow(val v: List<Long>)

@Serializable private data class IntMapRow(val v: Map<Long, String>)

@Serializable private data class TupleMapRow(val v: Map<List<Long>, String>)

@Serializable private data class RecordKey(val x: Long, val y: Long)

@Serializable private data class RecordMapRow(val v: Map<RecordKey, String>)

@Serializable private data class SetRow(val v: Set<Long>)

@Serializable private data class OptionRow(val v: Long?)

@Serializable
@JsonClassDiscriminator("tag")
private sealed class P {
    @Serializable
    @SerialName("X")
    data object X : P()

    @Serializable
    @SerialName("O")
    data object O : P()
}

@Serializable private data class SumSimpleRow(val v: P)

@Serializable
@JsonClassDiscriminator("tag")
private sealed class S {
    @Serializable
    @SerialName("Foo")
    data class Foo(val value: Long) : S()

    @Serializable
    @SerialName("Bar")
    data object Bar : S()
}

@Serializable private data class SumPayloadRow(val v: S)

private inline fun <reified T> decodeExample(json: String): T =
    Json.decodeFromString(ItfValueSerializer, json).decode()

private val rowKotlinTypeSubstring: Map<String, String> = mapOf(
    "int" to "Long",
    "int-bignum" to "@Contextual BigInteger",
    "bool" to "Boolean",
    "str" to "String",
    "tuple" to "List<Long>",
    "int-map" to "Map<Long, V>",
    "tuple-map" to "Map<List<Long>, V>",
    "record-map" to "Map<R, V>",
    "set" to "Set<T>",
    "option" to "T?",
    "sum-simple" to "sealed class",
    "sum-payload" to "sealed class",
)

private val exampleCases: Map<String, (String) -> Unit> = mapOf(
    "int" to { json -> assertEquals(IntRow(42), decodeExample<IntRow>(json)) },
    "int-bignum" to { json ->
        assertEquals(BigNumRow(BigInteger("123456789012345678901234567890")), decodeExample<BigNumRow>(json))
    },
    "bool" to { json -> assertEquals(BoolRow(true), decodeExample<BoolRow>(json)) },
    "str" to { json -> assertEquals(StrRow("hello"), decodeExample<StrRow>(json)) },
    "tuple" to { json -> assertEquals(TupleRow(listOf(1L, 2L)), decodeExample<TupleRow>(json)) },
    "int-map" to { json -> assertEquals(IntMapRow(mapOf(1L to "a", 2L to "b")), decodeExample<IntMapRow>(json)) },
    "tuple-map" to { json ->
        assertEquals(TupleMapRow(mapOf(listOf(1L, 2L) to "a")), decodeExample<TupleMapRow>(json))
    },
    "tuple-map-empty" to { json -> assertEquals(TupleMapRow(emptyMap()), decodeExample<TupleMapRow>(json)) },
    "record-map" to { json ->
        assertEquals(RecordMapRow(mapOf(RecordKey(1, 2) to "a")), decodeExample<RecordMapRow>(json))
    },
    "record-map-empty" to { json -> assertEquals(RecordMapRow(emptyMap()), decodeExample<RecordMapRow>(json)) },
    "set" to { json -> assertEquals(SetRow(setOf(1L, 2L, 3L)), decodeExample<SetRow>(json)) },
    "option-some" to { json -> assertEquals(OptionRow(42L), decodeExample<OptionRow>(json)) },
    "option-none" to { json -> assertEquals(OptionRow(null), decodeExample<OptionRow>(json)) },
    "sum-simple" to { json -> assertEquals(SumSimpleRow(P.X), decodeExample<SumSimpleRow>(json)) },
    "sum-payload" to { json -> assertEquals(SumPayloadRow(S.Foo(5)), decodeExample<SumPayloadRow>(json)) },
)

private data class TypeRow(val id: String, val kotlinType: String, val lineNumber: Int)

private val unescapedPipe = Regex("""(?<!\\)\|""")
private val exampleMarker = Regex("""<!--\s*example:([a-z0-9-]+)\s*-->\s*```json\s*(.*?)\s*```""", RegexOption.DOT_MATCHES_ALL)
private val rowIdMarker = Regex("""<!--id:([a-z0-9-]+)-->""")

private fun typesMdText(): String {
    val path = System.getProperty("quintKonnect.typesMdPath")
        ?: error(
            "System property 'quintKonnect.typesMdPath' is not set. Run this test through " +
                "Gradle (:itf:test), which wires it in itf/build.gradle.kts; an IDE run " +
                "configuration that bypasses Gradle won't have it.",
        )
    val file = File(path)
    check(file.isFile) { "types.md not found at '$path' (from quintKonnect.typesMdPath)" }
    return file.readText()
}

private fun parseRows(text: String): List<TypeRow> {
    val lines = text.lines()
    val headerIdx = lines.indexOfFirst { it.trim().startsWith("| Quint type") }
    check(headerIdx >= 0) { "types.md: couldn't find the '| Quint type | ...' table header" }
    val rows = mutableListOf<TypeRow>()
    var i = headerIdx + 2
    while (i < lines.size && lines[i].trimStart().startsWith("|")) {
        val line = lines[i]
        val cells = line.trim().removePrefix("|").split(unescapedPipe).map { it.trim() }
        check(cells.size >= 2) { "types.md line ${i + 1}: expected at least 2 cells, got: $line" }
        val id = rowIdMarker.find(line)?.groupValues?.get(1)
            ?: fail<String>(
                "types.md line ${i + 1} has no <!--id:...--> marker, so the doc test can't check " +
                    "it: $line",
            )
        rows += TypeRow(id, cells[1], i + 1)
        i++
    }
    return rows
}

private fun parseExamples(text: String): Map<String, String> =
    exampleMarker.findAll(text).associate { it.groupValues[1] to it.groupValues[2] }

class TypesMdDocTest {

    @Test
    fun `every table row has a matching Kotlin type and example`() {
        val text = typesMdText()
        val rows = parseRows(text)
        val examples = parseExamples(text)

        val rowIds = rows.map { it.id }.toSet()
        assertEquals(rows.size, rowIds.size, "duplicate <!--id:...--> markers in types.md: ${rows.map { it.id }}")

        val uncovered = rowIds.filter { rowId -> examples.keys.none { it == rowId || it.startsWith("$rowId-") } }
        assertTrue(
            uncovered.isEmpty(),
            "types.md row id(s) with no matching '## Examples' entry (a row was added without an " +
                "example): $uncovered",
        )

        val orphanExamples = examples.keys.filter { exampleId -> rowIds.none { exampleId == it || exampleId.startsWith("$it-") } }
        assertTrue(orphanExamples.isEmpty(), "'## Examples' entries with no matching table row id: $orphanExamples")

        assertEquals(
            examples.keys,
            exampleCases.keys,
            "exampleCases in TypesMdDocTest.kt must match types.md's '## Examples' ids exactly",
        )
        assertEquals(
            rowIds,
            rowKotlinTypeSubstring.keys,
            "rowKotlinTypeSubstring in TypesMdDocTest.kt must match types.md's row ids exactly",
        )

        for (row in rows) {
            val expectedSubstring = rowKotlinTypeSubstring.getValue(row.id)
            assertTrue(
                row.kotlinType.contains(expectedSubstring),
                "types.md line ${row.lineNumber}: Kotlin type cell '${row.kotlinType}' doesn't " +
                    "mention expected '$expectedSubstring'",
            )
        }
    }

    @TestFactory
    fun `decode every documented example`(): List<DynamicTest> {
        val examples = parseExamples(typesMdText())
        return examples.map { (id, json) ->
            dynamicTest(id) {
                val case = exampleCases[id] ?: fail("no exampleCases entry for '$id' in TypesMdDocTest.kt")
                case(json)
            }
        }
    }
}
