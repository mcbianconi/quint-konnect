package io.github.mcbianconi.quintkonnect.itf

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class ItfTrace(
    val vars: List<String> = emptyList(),
    val states: List<ItfState>,
)

data class ItfState(val value: LinkedHashMap<String, ItfValue>)

// https://apalache-mc.org/docs/adr/015adr-trace.html
fun parseTrace(json: String): ItfTrace {
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
