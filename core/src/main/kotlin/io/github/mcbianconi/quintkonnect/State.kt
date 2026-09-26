package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.itf.decode
import kotlinx.serialization.KSerializer

public interface State<D : Driver> {
    public fun check(driver: D, specValue: ItfValue)

    public companion object {
        public fun <D : Driver> disabled(): State<D> = object : State<D> {
            override fun check(driver: D, specValue: ItfValue) {}
        }
    }
}

public abstract class TypedState<D : Driver, S : Any>(
    private val serializer: KSerializer<S>,
) : State<D> {

    public abstract fun extractFromDriver(driver: D): S

    override fun check(driver: D, specValue: ItfValue) {
        val specState = specValue.decode(serializer)
        val driverState = extractFromDriver(driver)

        if (specState != driverState) {
            val fullValues = fullValueDiff(specState.toString(), driverState.toString())
            val fieldDiff = try {
                buildFieldDiff(serializer, specState, driverState)
            } catch (e: Exception) {
                null
            }
            val diff = if (fieldDiff.isNullOrEmpty()) fullValues else fieldDiff.joinToString("\n")
            throw IllegalStateException(
                "State invariant failed:\n$diff",
                IllegalStateException("Full values:\n$fullValues"),
            )
        }
    }
}

private fun fullValueDiff(spec: String, impl: String): String {
    val sb = StringBuilder()
    sb.appendLine("--- specification")
    sb.appendLine("+++ implementation")
    spec.lines().forEach { sb.appendLine("-$it") }
    impl.lines().forEach { sb.appendLine("+$it") }
    return sb.toString()
}
