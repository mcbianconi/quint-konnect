package io.github.mcbianconi.quintkonnect.itf

import kotlinx.serialization.json.*

/**
 * The [Json] instance used for all ITF deserialization.
 *
 * ignoreUnknownKeys is required because Quint emits a
 * `"value": []` field on unit sum-type variants (e.g. `{tag: "None", value: []}`), which has no
 * corresponding field in the Kotlin `data object` mapped to that variant.
 *
 * allowStructuredMapKeys is required because [toNormalizedJson] encodes a Quint map with a
 * tuple- or record-keyed type as a flat `[k1, v1, k2, v2, ...]` [JsonArray] (see below), which
 * kotlinx.serialization only decodes into `Map<K, V>` when this flag is set.
 */
val QuintJson = Json {
    ignoreUnknownKeys = true
    allowStructuredMapKeys = true
}

/**
 * Converts this [ItfValue] to a plain [JsonElement] that standard `@Serializable` data classes
 * can deserialize via kotlinx.serialization.
 *
 * The ITF encoding uses `#`-prefixed type tags that kotlinx.serialization doesn't understand
 * directly. This function strips those tags and produces idiomatic JSON:
 *
 * - [ItfValue.Tup] → [JsonArray] (map to `List<T>` in your state class; index 0 = `._1`, etc.)
 * - [ItfValue.Set] → [JsonArray] (map to `Set<T>` in your state class)
 * - [ItfValue.BigInt] → [JsonPrimitive] backed by [Long] if it fits, otherwise [java.math.BigDecimal]
 * - [ItfValue.Map] → [JsonObject] with string keys when every key is a primitive (`int`, `str`,
 *   `bool`), so `Map<Long, V>` / `Map<String, V>` / `Map<Boolean, V>` deserialization works exactly
 *   as before. Otherwise → a flat [JsonArray] `[k1, v1, k2, v2, ...]`, so `Map<List<Long>, V>`
 *   (tuple keys) or `Map<R, V>` (record keys, `R` a `@Serializable` data class) deserialize via
 *   [QuintJson]'s `allowStructuredMapKeys`.
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
    is ItfValue.Map    -> entries.toNormalizedMapJson()
    is ItfValue.Record -> JsonObject(fields.mapValues { it.value.toNormalizedJson() })
    is ItfValue.Unserializable -> JsonPrimitive(value)
}

private fun List<Pair<ItfValue, ItfValue>>.toNormalizedMapJson(): JsonElement =
    if (all { (key, _) -> key.isPrimitiveMapKey() }) {
        JsonObject(associate { (key, value) -> key.toJsonKey() to value.toNormalizedJson() })
    } else {
        JsonArray(flatMap { (key, value) -> listOf(key.toNormalizedJson(), value.toNormalizedJson()) })
    }

private fun ItfValue.isPrimitiveMapKey(): Boolean =
    this is ItfValue.Num || this is ItfValue.BigInt || this is ItfValue.Str || this is ItfValue.Bool

private fun ItfValue.toJsonKey(): String = when (this) {
    is ItfValue.Num    -> value.toString()
    is ItfValue.BigInt -> value
    is ItfValue.Str    -> value
    is ItfValue.Bool   -> value.toString()
    else -> error("Cannot use ${this::class.simpleName} as a JSON object key")
}
