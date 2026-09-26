@file:OptIn(ExperimentalSerializationApi::class)

package io.github.mcbianconi.quintkonnect.itf

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
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
 * - [ItfValue.Map] → [JsonObject] with string keys when the key type is primitive (`int`, `str`,
 *   `bool`) or an enum, so `Map<Long, V>` / `Map<String, V>` / `Map<Boolean, V>` deserialization
 *   works exactly as before. Otherwise → a flat [JsonArray] `[k1, v1, k2, v2, ...]`, so
 *   `Map<List<Long>, V>` (tuple keys) or `Map<R, V>` (record keys, `R` a `@Serializable` data
 *   class) deserialize via [QuintJson]'s `allowStructuredMapKeys`.
 * - All other variants → their natural JSON equivalent
 *
 * [descriptor], when given, is the [SerialDescriptor] of the Kotlin type this value is being
 * normalized for. It lets an empty [ItfValue.Map] pick the right JSON shape: an empty map can't
 * tell a primitive-keyed map from a tuple/record-keyed one from its entries alone, since it has
 * none. Passing `null` (the default) reproduces the pre-descriptor-aware behavior: primitive-key
 * detection falls back to inspecting the actual keys, so an empty complex-keyed map normalizes to
 * `{}` and only decodes into a primitive-keyed map.
 *
 * Descriptor propagation follows the target shape: element 0 of a `LIST`-kind descriptor for
 * [ItfValue.List]/[ItfValue.Tup]/[ItfValue.Set] elements, elements 0/1 of a `MAP`-kind descriptor
 * for map keys/values, and the named element of a `CLASS`/`OBJECT`-kind descriptor for record
 * fields.
 */
fun ItfValue.toNormalizedJson(descriptor: SerialDescriptor? = null): JsonElement = when (this) {
    is ItfValue.Bool   -> JsonPrimitive(value)
    is ItfValue.Num    -> JsonPrimitive(value)
    is ItfValue.Str    -> JsonPrimitive(value)
    is ItfValue.BigInt -> JsonPrimitive(value.toLongOrNull() ?: value.toBigDecimal())
    is ItfValue.List   -> JsonArray(values.map { it.toNormalizedJson(descriptor?.collectionElementDescriptor()) })
    is ItfValue.Tup    -> JsonArray(values.map { it.toNormalizedJson(descriptor?.collectionElementDescriptor()) })
    is ItfValue.Set    -> JsonArray(values.map { it.toNormalizedJson(descriptor?.collectionElementDescriptor()) })
    is ItfValue.Map    -> entries.toNormalizedMapJson(descriptor)
    is ItfValue.Record -> JsonObject(fields.mapValues { (name, v) -> v.toNormalizedJson(descriptor?.fieldDescriptor(name)) })
    is ItfValue.Unserializable -> JsonPrimitive(value)
}

private fun List<Pair<ItfValue, ItfValue>>.toNormalizedMapJson(descriptor: SerialDescriptor?): JsonElement {
    val mapDescriptor = descriptor?.takeIf { it.kind == StructureKind.MAP }
    val keyDescriptor = mapDescriptor?.getElementDescriptor(0)
    val valueDescriptor = mapDescriptor?.getElementDescriptor(1)
    val usesPrimitiveKeys = keyDescriptor?.isPrimitiveOrEnum()
        ?: all { (key, _) -> key.isPrimitiveMapKey() }

    return if (usesPrimitiveKeys) {
        JsonObject(associate { (key, value) -> key.toJsonKey() to value.toNormalizedJson(valueDescriptor) })
    } else {
        JsonArray(flatMap { (key, value) -> listOf(key.toNormalizedJson(keyDescriptor), value.toNormalizedJson(valueDescriptor)) })
    }
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

private fun SerialDescriptor.isPrimitiveOrEnum(): Boolean = kind is PrimitiveKind || kind == SerialKind.ENUM

private fun SerialDescriptor.collectionElementDescriptor(): SerialDescriptor? =
    if (kind == StructureKind.LIST) getElementDescriptor(0) else null

private fun SerialDescriptor.fieldDescriptor(name: String): SerialDescriptor? {
    if (kind != StructureKind.CLASS && kind != StructureKind.OBJECT) return null
    val index = getElementIndex(name)
    return if (index == CompositeDecoder.UNKNOWN_NAME) null else getElementDescriptor(index)
}
