package io.github.mcbianconi.quintkonnect.itf

fun ItfValue.display(): String = when (this) {
    is ItfValue.Bool   -> value.toString()
    is ItfValue.Num    -> value.toString()
    is ItfValue.Str    -> "\"$value\""
    is ItfValue.BigInt -> value
    is ItfValue.List   -> "List(${values.joinToString(", ") { it.display() }})"
    is ItfValue.Tup    -> "(${values.joinToString(", ") { it.display() }})"
    is ItfValue.Set    -> "Set(${values.joinToString(", ") { it.display() }})"
    is ItfValue.Map    -> "Map(${entries.joinToString(", ") { (k, v) -> "${k.display()} -> ${v.display()}" }})"
    is ItfValue.Record -> displayRecord(fields)
    is ItfValue.Unserializable -> value
}

private fun displayRecord(fields: LinkedHashMap<String, ItfValue>): String {
    val tag = fields["tag"]
    val value = fields["value"]
    return if (fields.size == 2 && tag is ItfValue.Str && value != null) {
        val valueStr = when {
            value is ItfValue.Tup && value.values.isEmpty() -> ""
            value is ItfValue.Tup -> value.display()
            else -> "(${value.display()})"
        }
        "${tag.value}$valueStr"
    } else {
        "{ ${fields.entries.joinToString(", ") { (k, v) -> "$k: ${v.display()}" }} }"
    }
}
