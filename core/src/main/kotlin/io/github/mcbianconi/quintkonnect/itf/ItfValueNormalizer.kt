package io.github.mcbianconi.quintkonnect.itf

import kotlinx.serialization.json.*

/**
 * The [Json] instance used for all ITF deserialization.
 *
 * ignoreUnknownKeys is required because Quint emits a
 * `"value": []` field on unit sum-type variants (e.g. `{tag: "None", value: []}`), which has no
 * corresponding field in the Kotlin `data object` mapped to that variant.
 */
val QuintJson = Json { ignoreUnknownKeys = true }

/**
 * Converts this [ItfValue] to a plain [JsonElement] that standard `@Serializable` data classes
 * can deserialize via kotlinx.serialization.
 *
 * The ITF encoding uses `#`-prefixed type tags that kotlinx.serialization doesn't understand
 * directly. This function strips those tags and produces idiomatic JSON:
 *
 * - [ItfValue.Tup] → [JsonArray] (so `List<T>` and `Pair<A,B>` deserialization work by index)
 * - [ItfValue.Set] → [JsonArray] (map to `List<T>` in your state class)
 * - [ItfValue.BigInt] → [JsonPrimitive] backed by [Long] if it fits, otherwise [java.math.BigDecimal]
 * - [ItfValue.Map] → [JsonObject] with string keys (so `Map<Long, V>` deserialization works)
 * - All other variants → their natural JSON equivalent
 */
fun ItfValue.toNormalizedJson(): JsonElement = when (this) {
    is ItfValue.Bool   -> JsonPrimitive(value)
    is ItfValue.Num    -> JsonPrimitive(value)
    is ItfValue.Str    -> JsonPrimitive(value)
    is ItfValue.BigInt -> JsonPrimitive(value.toLongOrNull() ?: value.toBigDecimal())
    is ItfValue.List   -> JsonArray(values.map { it.toNormalizedJson() })
    is ItfValue.Tup    -> JsonArray(values.map { it.toNormalizedJson() })
    is ItfValue.Set    -> JsonArray(values.map { it.toNormalizedJson() })
    is ItfValue.Map    -> JsonObject(entries.associate { (k, v) -> k.toJsonKey() to v.toNormalizedJson() })
    is ItfValue.Record -> JsonObject(fields.mapValues { it.value.toNormalizedJson() })
    is ItfValue.Unserializable -> JsonPrimitive(value)
}

private fun ItfValue.toJsonKey(): String = when (this) {
    is ItfValue.Num    -> value.toString()
    is ItfValue.BigInt -> value
    is ItfValue.Str    -> value
    is ItfValue.Bool   -> value.toString()
    else -> error("Cannot use ${this::class.simpleName} as a JSON object key")
}
