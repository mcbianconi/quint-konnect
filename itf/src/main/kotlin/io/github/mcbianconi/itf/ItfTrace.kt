package io.github.mcbianconi.itf

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

public data class ItfTrace(
    public val vars: List<String> = emptyList(),
    public val states: List<ItfState>,
)

public data class ItfState(public val value: LinkedHashMap<String, ItfValue>)

// https://apalache-mc.org/docs/adr/015adr-trace.html
public fun parseTrace(json: String): ItfTrace {
    val root = Json.parseToJsonElement(json).jsonObject
    val vars = root["vars"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
    val states = root["states"]?.jsonArray?.map { stateEl ->
        val obj = stateEl.jsonObject
        val fields = LinkedHashMap<String, ItfValue>()
        for ((k, v) in obj) {
            if (k != "#meta") fields[k] = ItfValueSerializer.fromJsonElement(v)
        }
        ItfState(fields)
    } ?: emptyList()
    return ItfTrace(vars = vars, states = states)
}
