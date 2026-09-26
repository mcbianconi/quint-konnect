package io.github.mcbianconi.quintkonnect.itf

import kotlin.collections.List as KList

/**
 * Typed representation of a value in the [Informal Trace Format (ITF)](https://apalache-mc.org/docs/adr/015adr-trace.html).
 *
 * ITF is the JSON-based format used by Quint and Apalache to encode execution traces. Because JSON has
 * fewer types than TLA+/Quint, ITF uses `#`-prefixed object keys as type tags:
 *
 * | ITF JSON                        | [ItfValue] variant  |
 * |---------------------------------|---------------------|
 * | `true` / `false`                | [Bool]              |
 * | `42`                            | [Num]               |
 * | `"hello"`                       | [Str]               |
 * | `{"#bigint": "123"}`            | [BigInt]            |
 * | `[1, 2, 3]`                     | [List]              |
 * | `{"#tup": [1, 2]}`              | [Tup]               |
 * | `{"#set": [1, 2]}`              | [Set]               |
 * | `{"#map": [[k, v], ...]}`       | [Map]               |
 * | `{"field": ...}`                | [Record]            |
 *
 * Use [ItfValueSerializer] to deserialize ITF JSON into [ItfValue]. Use [toNormalizedJson] to convert
 * an [ItfValue] into a plain [kotlinx.serialization.json.JsonElement] suitable for standard
 * `@Serializable` data classes.
 */
sealed class ItfValue {
    /** A boolean value. */
    data class Bool(val value: Boolean) : ItfValue()

    /** A 64-bit integer. Quint `int` values that fit in a [Long] are encoded as plain JSON numbers. */
    data class Num(val value: Long) : ItfValue()

    /** A string value. */
    data class Str(val value: String) : ItfValue()

    /**
     * An arbitrary-precision integer encoded as `{"#bigint": "123"}`.
     *
     * [value] is the decimal string representation. [toNormalizedJson] converts this to a
     * [kotlinx.serialization.json.JsonPrimitive] backed by [Long] when the value fits, otherwise
     * [java.math.BigInteger]; decode a field of this type with
     * `@Serializable(with = BigIntegerSerializer::class) val n: BigInteger`.
     */
    data class BigInt(val value: String) : ItfValue()

    /** A Quint `List[T]`, encoded as a plain JSON array `[...]`. */
    data class List(val values: KList<ItfValue>) : ItfValue()

    /**
     * A Quint tuple, encoded as `{"#tup": [...]}`.
     *
     * Tuple element `._1` is at index 0, `._2` at index 1, etc. [toNormalizedJson] converts this
     * to a plain JSON array so that `@Serializable` Kotlin [kotlin.collections.List] types can
     * deserialize it directly.
     */
    data class Tup(val values: KList<ItfValue>) : ItfValue()

    /**
     * A Quint `Set[T]`, encoded as `{"#set": [...]}`.
     *
     * The element order in the array is unspecified, so map this to a Kotlin
     * [kotlin.collections.Set] in your `@Serializable` state class, not a
     * [kotlin.collections.List]; ITF gives no element order to preserve, and a `List` would fail
     * state comparison for correct implementations that happen to enumerate elements differently.
     * [toNormalizedJson] converts this to a plain JSON array, which kotlinx.serialization decodes
     * into `Set<T>` directly.
     */
    data class Set(val values: KList<ItfValue>) : ItfValue()

    /**
     * A Quint map (`T -> V`), encoded as `{"#map": [[k, v], ...]}`.
     *
     * [toNormalizedJson] converts this to a JSON object with string keys when the key type is
     * primitive (`int`, `str`, `bool`) or an enum, enabling `Map<Long, V>` / `Map<String, V>` /
     * `Map<Boolean, V>` deserialization. For a tuple, record or sum-type key, it instead converts
     * to a flat `[k1, v1, k2, v2, ...]` JSON array, decoded via `QuintJson`'s
     * `allowStructuredMapKeys` into `Map<List<Long>, V>` (tuple keys) or `Map<R, V>` (record/sum
     * keys, `R` a `@Serializable` type).
     */
    data class Map(val entries: KList<Pair<ItfValue, ItfValue>>) : ItfValue()

    /**
     * A Quint record or sum-type variant, encoded as a plain JSON object `{"field": ...}`.
     *
     * Sum-type variants emitted by Quint have the shape `{"tag": "VariantName", "value": ...}`.
     * Use [intoOption] to unwrap Quint's built-in `Option` type from this representation.
     *
     * The `#meta` key, if present in the raw JSON, is stripped during parsing.
     */
    data class Record(val fields: LinkedHashMap<String, ItfValue>) : ItfValue()

    /**
     * A value that could not be serialized by the Quint CLI, represented as a raw string.
     *
     * This is a fallback variant; encountering it usually indicates an unsupported Quint type.
     */
    data class Unserializable(val value: String) : ItfValue()
}

/**
 * Unwraps a Quint `Option` value.
 *
 * Quint's `Option[T]` is represented as a record with a `tag` field:
 * - `{tag: "Some", value: v}` → returns `v`
 * - `{tag: "None"}` → returns `null`
 * - Any other value → returns `this` unchanged (not an Option)
 */
fun ItfValue.intoOption(): ItfValue? = when {
    this is ItfValue.Record -> {
        val tag = fields["tag"]
        when {
            tag is ItfValue.Str && tag.value == "Some" -> fields["value"]
            tag is ItfValue.Str && tag.value == "None" -> null
            else -> this
        }
    }
    else -> this
}
