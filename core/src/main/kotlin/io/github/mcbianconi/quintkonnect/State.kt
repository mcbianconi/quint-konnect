package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.quintkonnect.itf.ItfValue
import io.github.mcbianconi.quintkonnect.itf.QuintJson
import io.github.mcbianconi.quintkonnect.itf.toNormalizedJson
import kotlinx.serialization.KSerializer

interface State<D : Driver> {
    fun check(driver: D, specValue: ItfValue)

    companion object {
        fun <D : Driver> disabled(): State<D> = object : State<D> {
            override fun check(driver: D, specValue: ItfValue) {}
        }
    }
}

abstract class TypedState<D : Driver, S : Any>(
    private val serializer: KSerializer<S>,
) : State<D> {

    abstract fun extractFromDriver(driver: D): S

    override fun check(driver: D, specValue: ItfValue) {
        val specState = QuintJson.decodeFromJsonElement(serializer, specValue.toNormalizedJson())
        val driverState = extractFromDriver(driver)

        if (specState != driverState) {
            val diff = buildDiff(specState.toString(), driverState.toString())
            error("State invariant failed:\n$diff")
        }
    }

    private fun buildDiff(spec: String, impl: String): String {
        val sb = StringBuilder()
        sb.appendLine("--- specification")
        sb.appendLine("+++ implementation")
        spec.lines().forEach { sb.appendLine("-$it") }
        impl.lines().forEach { sb.appendLine("+$it") }
        return sb.toString()
    }
}
