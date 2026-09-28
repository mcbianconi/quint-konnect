package io.github.mcbianconi.itf

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * [kotlinx.serialization] serializer for [ItfValue].
 *
 * Handles the ITF JSON encoding where Quint uses `#`-prefixed keys to distinguish types that JSON
 * cannot represent natively. Pass this serializer when decoding raw ITF JSON:
 *
 * ```kotlin
 * val value = Json.decodeFromString(ItfValueSerializer, jsonString)
 * ```
 *
 * The mapping between JSON and [ItfValue] variants:
 * - JSON primitives → [ItfValue.Bool], [ItfValue.Num], [ItfValue.Str]
 * - Plain JSON arrays → [ItfValue.List]
 * - `{"#bigint": "123"}` → [ItfValue.BigInt]
 * - `{"#tup": [...]}` → [ItfValue.Tup]
 * - `{"#set": [...]}` → [ItfValue.Set]
 * - `{"#map": [[k,v], ...]}` → [ItfValue.Map]
 * - `{"#unserializable": "..."}` → [ItfValue.Unserializable] (ITF ADR-015)
 * - Other JSON objects (stripping `#meta`) → [ItfValue.Record]
 *
 * Serialization (write path) is the inverse of the above.
 *
 * @see [Kotlinx serialization guide](https://kotlinlang.org/docs/serialization-guide.html)
 */
public object ItfValueSerializer : KSerializer<ItfValue> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ItfValue")

    override fun serialize(encoder: Encoder, value: ItfValue) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("ItfValueSerializer only supports JSON")
        jsonEncoder.encodeJsonElement(toJsonElement(value))
    }

    override fun deserialize(decoder: Decoder): ItfValue {
        val jsonDecoder = decoder as? JsonDecoder ?: error("ItfValueSerializer only supports JSON")
        return fromJsonElement(jsonDecoder.decodeJsonElement())
    }

    public fun fromJsonElement(element: JsonElement): ItfValue = when (element) {
        is JsonPrimitive -> fromPrimitive(element)
        is JsonArray -> ItfValue.List(element.map { fromJsonElement(it) })
        is JsonObject -> fromObject(element)
    }

    private fun fromPrimitive(p: JsonPrimitive): ItfValue = when {
        p.isString -> ItfValue.Str(p.content)
        p.content == "true" -> ItfValue.Bool(true)
        p.content == "false" -> ItfValue.Bool(false)
        else -> ItfValue.Num(p.long)
    }

    private fun fromObject(obj: JsonObject): ItfValue {
        val keys = obj.keys
        return when {
            keys.size == 1 && "#bigint" in obj -> {
                ItfValue.BigInt(obj["#bigint"]!!.jsonPrimitive.content)
            }

            keys.size == 1 && "#tup" in obj -> {
                ItfValue.Tup(obj["#tup"]!!.jsonArray.map { fromJsonElement(it) })
            }

            keys.size == 1 && "#set" in obj -> {
                ItfValue.Set(obj["#set"]!!.jsonArray.map { fromJsonElement(it) })
            }

            keys.size == 1 && "#map" in obj -> {
                val entries = obj["#map"]!!.jsonArray.map { pair ->
                    val arr = pair.jsonArray
                    fromJsonElement(arr[0]) to fromJsonElement(arr[1])
                }
                ItfValue.Map(entries)
            }

            keys.size == 1 && "#unserializable" in obj -> {
                ItfValue.Unserializable(obj["#unserializable"]!!.jsonPrimitive.content)
            }

            else -> {
                val fields = LinkedHashMap<String, ItfValue>()
                for ((k, v) in obj) {
                    if (k != "#meta") fields[k] = fromJsonElement(v)
                }
                ItfValue.Record(fields)
            }
        }
    }

    public fun toJsonElement(value: ItfValue): JsonElement = when (value) {
        is ItfValue.Bool -> JsonPrimitive(value.value)

        is ItfValue.Num -> JsonPrimitive(value.value)

        is ItfValue.Str -> JsonPrimitive(value.value)

        is ItfValue.BigInt -> buildJsonObject { put("#bigint", value.value) }

        is ItfValue.List -> JsonArray(value.values.map { toJsonElement(it) })

        is ItfValue.Tup -> buildJsonObject { put("#tup", JsonArray(value.values.map { toJsonElement(it) })) }

        is ItfValue.Set -> buildJsonObject { put("#set", JsonArray(value.values.map { toJsonElement(it) })) }

        is ItfValue.Map -> buildJsonObject {
            put(
                "#map",
                JsonArray(
                    value.entries.map { (k, v) ->
                        JsonArray(listOf(toJsonElement(k), toJsonElement(v)))
                    },
                ),
            )
        }

        is ItfValue.Record -> buildJsonObject {
            for ((k, v) in value.fields) put(k, toJsonElement(v))
        }

        is ItfValue.Unserializable -> buildJsonObject { put("#unserializable", value.value) }
    }
}
