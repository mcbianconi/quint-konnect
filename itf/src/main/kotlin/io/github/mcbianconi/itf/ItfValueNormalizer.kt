@file:OptIn(ExperimentalSerializationApi::class, SealedSerializationApi::class)

package io.github.mcbianconi.itf

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SealedSerializationApi
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
 * - [ItfValue.BigInt] → [JsonPrimitive] backed by [Long] if it fits, otherwise [java.math.BigInteger]
 *   (decode with [BigIntegerSerializer])
 * - [ItfValue.Map] → [JsonObject] with string keys when the key type is primitive (`int`, `str`,
 *   `bool`) or an enum, so `Map<Long, V>` / `Map<String, V>` / `Map<Boolean, V>` deserialization
 *   works exactly as before. Otherwise → a flat [JsonArray] `[k1, v1, k2, v2, ...]`, so
 *   `Map<List<Long>, V>` (tuple keys) or `Map<R, V>` (record keys, `R` a `@Serializable` data
 *   class) deserialize via [QuintJson]'s `allowStructuredMapKeys`.
 * - A record whose target [descriptor] is nullable is treated as a Quint `Option[T]`:
 *   `{tag: "None"}` → `null`, `{tag: "Some", value: v}` → `v` normalized against the non-null
 *   descriptor. A record whose target descriptor isn't nullable is never unwrapped this way, so a
 *   user sum type with `Some`/`None` variants decodes as-is.
 * - All other variants → their natural JSON equivalent
 *
 * [descriptor], when given, is the [SerialDescriptor] of the Kotlin type this value is being
 * normalized for. It lets an empty [ItfValue.Map] pick the right JSON shape (an empty map can't
 * tell a primitive-keyed map from a tuple/record-keyed one from its entries alone, since it has
 * none) and lets a `null`-able Kotlin field unwrap a Quint `Option[T]`. Passing `null` (the
 * default) reproduces the pre-descriptor-aware behavior: primitive-key detection falls back to
 * inspecting the actual keys, an empty complex-keyed map normalizes to `{}` (and so only decodes
 * into a primitive-keyed map), and no field is treated as an `Option`.
 *
 * Descriptor propagation follows the target shape: element 0 of a `LIST`-kind descriptor for
 * [ItfValue.List]/[ItfValue.Tup]/[ItfValue.Set] elements, elements 0/1 of a `MAP`-kind descriptor
 * for map keys/values, and the named element of a `CLASS`/`OBJECT`-kind descriptor for record
 * fields. It doesn't descend into a sealed class's variants, so a field nested inside a Quint sum
 * type variant's payload normalizes as if no descriptor were given.
 */
fun ItfValue.toNormalizedJson(descriptor: SerialDescriptor? = null): JsonElement = when (this) {
    is ItfValue.Bool   -> JsonPrimitive(value)
    is ItfValue.Num    -> JsonPrimitive(value)
    is ItfValue.Str    -> JsonPrimitive(value)
    is ItfValue.BigInt -> JsonPrimitive(value.toLongOrNull() ?: value.toBigInteger())
    is ItfValue.List   -> JsonArray(values.map { it.toNormalizedJson(descriptor?.collectionElementDescriptor()) })
    is ItfValue.Tup    -> JsonArray(values.map { it.toNormalizedJson(descriptor?.collectionElementDescriptor()) })
    is ItfValue.Set    -> JsonArray(values.map { it.toNormalizedJson(descriptor?.collectionElementDescriptor()) })
    is ItfValue.Map    -> entries.toNormalizedMapJson(descriptor)
    is ItfValue.Record -> toNormalizedRecordJson(descriptor)
    is ItfValue.Unserializable -> JsonPrimitive(value)
}

private fun ItfValue.Record.toNormalizedRecordJson(descriptor: SerialDescriptor?): JsonElement {
    if (descriptor != null && descriptor.isNullable) {
        val nonNullDescriptor = descriptor.asNonNullable()
        return when (val unwrapped = intoOption()) {
            null -> JsonNull
            this -> normalizeFields(nonNullDescriptor)
            else -> unwrapped.toNormalizedJson(nonNullDescriptor)
        }
    }
    return normalizeFields(descriptor)
}

private fun ItfValue.Record.normalizeFields(descriptor: SerialDescriptor?): JsonElement =
    JsonObject(fields.mapValues { (name, v) -> v.toNormalizedJson(descriptor?.fieldDescriptor(name)) })

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

private fun SerialDescriptor.asNonNullable(): SerialDescriptor {
    val original = this
    return object : SerialDescriptor by original {
        override val isNullable: Boolean get() = false
    }
}
