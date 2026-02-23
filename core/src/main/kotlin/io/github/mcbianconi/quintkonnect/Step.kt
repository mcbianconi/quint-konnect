package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.itf.ItfValue
import io.github.mcbianconi.quintkonnect.itf.display
import io.github.mcbianconi.quintkonnect.nondet.NondetPicks

class Step(
    val actionTaken: String,
    val nondetPicks: NondetPicks,
    internal val state: ItfValue,
) {
    override fun toString(): String {
        val action = if (actionTaken.isEmpty()) "<anonymous>" else actionTaken
        val nondets = if (nondetPicks.isEmpty()) "<none>" else "\n$nondetPicks"
        val stateStr = when (state) {
            is ItfValue.Record -> {
                if (state.fields.isEmpty()) " <none>"
                else state.fields.entries.joinToString("") { (k, v) -> "\n+ $k: ${v.display()}" }
            }
            is ItfValue.Map -> {
                if (state.entries.isEmpty()) " <none>"
                else state.entries.joinToString("") { (k, v) -> "\n+ ${k.display()}: ${v.display()}" }
            }
            else -> " ${state.display()}"
        }
        return "Action taken: $action\nNondet picks:$nondets\nNext state:$stateStr"
    }

    companion object {
        fun fromState(state: LinkedHashMap<String, ItfValue>, config: DriverConfig): Step =
            if (config.nondetPath.isEmpty()) {
                extractFromMbtVars(state, config.statePath)
            } else {
                extractFromSumType(state, config.nondetPath, config.statePath)
            }
    }
}

private fun extractFromMbtVars(state: LinkedHashMap<String, ItfValue>, statePath: List<String>): Step {
    val actionTaken = state.remove("mbt::actionTaken")
        ?.let { it as? ItfValue.Str }?.value
        ?: error("Missing `mbt::actionTaken` variable in the trace")

    val nondetValue = state.remove("mbt::nondetPicks")
        ?: error("Missing `mbt::nondetPicks` variable in the trace")

    val nondetPicks = NondetPicks.fromItfValue(nondetValue)
    val stateValue = extractValueAtPath(state, statePath)
    return Step(actionTaken, nondetPicks, stateValue)
}

private fun extractFromSumType(
    state: LinkedHashMap<String, ItfValue>,
    nondetPath: List<String>,
    statePath: List<String>,
): Step {
    val sumRecord = findRecordAtPath(state, nondetPath)
    val actionTaken = (sumRecord["tag"] as? ItfValue.Str)?.value
        ?: error("Expected action to be a sum type variant. Value found: ${ItfValue.Record(sumRecord).display()}")

    val nondetPicks = when (val v = sumRecord["value"]) {
        is ItfValue.Tup    -> if (v.values.isEmpty()) NondetPicks.empty()
                              else error("Expected empty tuple for unit sum type variant")
        is ItfValue.Record -> NondetPicks.fromRecord(v.fields)
        else -> error(
            "Expected nondet picks to be a sum type variant value as a record.\n" +
                "Value found: ${ItfValue.Record(sumRecord).display()}"
        )
    }

    state.remove("mbt::actionTaken")
    state.remove("mbt::nondetPicks")

    val stateValue = extractValueAtPath(state, statePath)
    return Step(actionTaken, nondetPicks, stateValue)
}

private fun extractValueAtPath(state: LinkedHashMap<String, ItfValue>, path: List<String>): ItfValue {
    var current: ItfValue = ItfValue.Record(state)
    for (segment in path) {
        val record = (current as? ItfValue.Record)
            ?: error("Cannot read '$segment' from non-record value in path: $path\nCurrent value: ${current.display()}")
        current = record.fields[segment]
            ?: error("Cannot find a value at '$segment' in path: $path\nCurrent value: ${current.display()}")
    }
    return current
}

private fun findRecordAtPath(
    state: LinkedHashMap<String, ItfValue>,
    path: List<String>,
): LinkedHashMap<String, ItfValue> {
    var current: LinkedHashMap<String, ItfValue> = state
    for (segment in path) {
        val next = current[segment]
        current = (next as? ItfValue.Record)?.fields
            ?: error(
                "Cannot find a Record at '$segment' in path: $path\n" +
                    "Current state: ${ItfValue.Record(current).display()}"
            )
    }
    return current
}
