package io.github.mcbianconi.itf

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
 * Use [ItfValueSerializer] to deserialize ITF JSON into [ItfValue]. Use [decode] to decode an
 * [ItfValue] into a standard `@Serializable` type.
 */
public sealed class ItfValue {
    /** A boolean value. */
    public data class Bool(public val value: Boolean) : ItfValue()

    /** A 64-bit integer. Quint `int` values that fit in a [Long] are encoded as plain JSON numbers. */
    public data class Num(public val value: Long) : ItfValue()

    /** A string value. */
    public data class Str(public val value: String) : ItfValue()

    /**
     * An arbitrary-precision integer encoded as `{"#bigint": "123"}`.
     *
     * [value] is the decimal string representation. [decode] this into a [Long] field when the
     * value fits, otherwise into a `@Serializable(with = BigIntegerSerializer::class) val n:
     * BigInteger` field.
     */
    public data class BigInt(public val value: String) : ItfValue()

    /** A Quint `List[T]`, encoded as a plain JSON array `[...]`. */
    public data class List(public val values: KList<ItfValue>) : ItfValue()

    /**
     * A Quint tuple, encoded as `{"#tup": [...]}`.
     *
     * Tuple element `._1` is at index 0, `._2` at index 1, etc. [decode] a field of this type into
     * a [kotlin.collections.List].
     */
    public data class Tup(public val values: KList<ItfValue>) : ItfValue()

    /**
     * A Quint `Set[T]`, encoded as `{"#set": [...]}`.
     *
     * The element order in the array is unspecified, so map this to a Kotlin
     * [kotlin.collections.Set] in your `@Serializable` state class, not a
     * [kotlin.collections.List]; ITF gives no element order to preserve, and a `List` would fail
     * state comparison for correct implementations that happen to enumerate elements differently.
     * [decode] a field of this type into `Set<T>` directly.
     */
    public data class Set(public val values: KList<ItfValue>) : ItfValue()

    /**
     * A Quint map (`T -> V`), encoded as `{"#map": [[k, v], ...]}`.
     *
     * [decode] a field of this type into `Map<Long, V>` / `Map<String, V>` / `Map<Boolean, V>`
     * when the key type is primitive (`int`, `str`, `bool`) or an enum. For a tuple, record or
     * sum-type key, decode into `Map<List<Long>, V>` (tuple keys) or `Map<R, V>` (record/sum keys,
     * `R` a `@Serializable` type) instead.
     */
    public data class Map(public val entries: KList<Pair<ItfValue, ItfValue>>) : ItfValue()

    /**
     * A Quint record or sum-type variant, encoded as a plain JSON object `{"field": ...}`.
     *
     * Sum-type variants emitted by Quint have the shape `{"tag": "VariantName", "value": ...}`.
     * Use [intoOption] to unwrap Quint's built-in `Option` type from this representation.
     *
     * The `#meta` key, if present in the raw JSON, is stripped during parsing.
     */
    public data class Record(public val fields: LinkedHashMap<String, ItfValue>) : ItfValue()

    /**
     * A value that could not be serialized by the Quint CLI, represented as a raw string.
     *
     * This is a fallback variant; encountering it usually indicates an unsupported Quint type.
     */
    public data class Unserializable(public val value: String) : ItfValue()
}

/**
 * Unwraps a Quint `Option` value.
 *
 * Quint's `Option[T]` is represented as a record with a `tag` field:
 * - `{tag: "Some", value: v}` → returns `v`
 * - `{tag: "None"}` → returns `null`
 * - Any other value → returns `this` unchanged (not an Option)
 */
public fun ItfValue.intoOption(): ItfValue? = when {
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
