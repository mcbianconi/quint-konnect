package io.github.mcbianconi.itf

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.math.BigInteger

/**
 * [kotlinx.serialization] serializer for [BigInteger], for Quint `int` values that don't fit in a
 * [Long].
 *
 * Annotate the target field with `@Serializable(with = BigIntegerSerializer::class) val n:
 * BigInteger` to decode it; a plain `Long` field keeps working as before for values that fit.
 * Supports both [ItfValue.decode] (reading the [ItfValue.Num]/[ItfValue.BigInt] directly) and a
 * plain [kotlinx.serialization.json.Json] (reading the JSON element directly), since it's public
 * API that isn't restricted to ITF decoding.
 */
public object BigIntegerSerializer : KSerializer<BigInteger> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigInteger) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("BigIntegerSerializer only supports JSON")
        jsonEncoder.encodeJsonElement(JsonPrimitive(value))
    }

    override fun deserialize(decoder: Decoder): BigInteger = when (decoder) {
        is ItfValueDecoder -> decoder.rawValue.toBigIntegerValue(decoder.rawPath)
        is JsonDecoder -> decoder.decodeJsonElement().jsonPrimitive.content.toBigInteger()
        else -> error("BigIntegerSerializer only supports JSON or ITF decoding")
    }
}

private fun ItfValue.toBigIntegerValue(path: String): BigInteger = when (this) {
    is ItfValue.Num -> BigInteger.valueOf(value)
    is ItfValue.BigInt -> value.toBigInteger()
    else -> throw ItfDecodingException(
        "${path.ifEmpty { "<root>" }}: expected an int, got ${this::class.simpleName}: ${display()}",
    )
}
