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
 * [decode] normalizes such a value to an unquoted JSON number before deserializing it. Annotate the
 * target field with `@Serializable(with = BigIntegerSerializer::class) val n: BigInteger` to
 * decode it; a plain `Long` field keeps working as before for values that fit.
 */
object BigIntegerSerializer : KSerializer<BigInteger> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BigInteger", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: BigInteger) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("BigIntegerSerializer only supports JSON")
        jsonEncoder.encodeJsonElement(JsonPrimitive(value))
    }

    override fun deserialize(decoder: Decoder): BigInteger {
        val jsonDecoder = decoder as? JsonDecoder ?: error("BigIntegerSerializer only supports JSON")
        return jsonDecoder.decodeJsonElement().jsonPrimitive.content.toBigInteger()
    }
}
