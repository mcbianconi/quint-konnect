package io.github.mcbianconi.itf

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.math.BigInteger

@Serializable
private data class Counter(val count: Long, val label: String)

@Serializable
private data class HugeCount(val n: @Serializable(with = BigIntegerSerializer::class) BigInteger)

@Serializable
private data class NullableCount(val n: Long?)

class ItfValueDecodeTest {

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
            )
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
}
