package io.github.mcbianconi.quintkonnect.nondet

import io.github.mcbianconi.quintkonnect.itf.ItfValue
import io.github.mcbianconi.quintkonnect.itf.QuintJson
import io.github.mcbianconi.quintkonnect.itf.display
import io.github.mcbianconi.quintkonnect.itf.intoOption
import io.github.mcbianconi.quintkonnect.itf.toNormalizedJson
import kotlinx.serialization.json.decodeFromJsonElement

class NondetPicks(private val picks: LinkedHashMap<String, ItfValue>) {

    fun get(name: String): ItfValue? = picks[name]

    fun isEmpty(): Boolean = picks.isEmpty()

    override fun toString(): String =
        picks.entries.joinToString("\n") { (k, v) -> "+ $k: ${v.display()}" }

    companion object {
        fun empty(): NondetPicks = NondetPicks(LinkedHashMap())

        fun fromItfValue(value: ItfValue): NondetPicks {
            require(value is ItfValue.Record) {
                "Expected nondet picks to be a Record, got: ${value.display()}"
            }
            return fromRecord(value.fields)
        }

        fun fromRecord(record: LinkedHashMap<String, ItfValue>): NondetPicks {
            val picks = LinkedHashMap<String, ItfValue>()
            for ((key, v) in record) {
                val unwrapped = v.intoOption()
                if (unwrapped != null) picks[key] = unwrapped
            }
            return NondetPicks(picks)
        }
    }
}

inline fun <reified T> NondetPicks.decode(name: String): T {
    val itfValue = get(name) ?: error("Missing required nondet pick '$name'")
    return QuintJson.decodeFromJsonElement(itfValue.toNormalizedJson())
}

inline fun <reified T> NondetPicks.decodeOrNull(name: String): T? {
    val itfValue = get(name) ?: return null
    return QuintJson.decodeFromJsonElement(itfValue.toNormalizedJson())
}
