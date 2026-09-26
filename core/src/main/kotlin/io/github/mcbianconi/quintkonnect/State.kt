package io.github.mcbianconi.quintkonnect

import io.github.mcbianconi.itf.ItfValue
import io.github.mcbianconi.itf.decode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException

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
        val specState = try {
            specValue.decode(serializer)
        } catch (e: SerializationException) {
            throw prefixDecodeError("state", e)
        }
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

/**
 * Prefixes an [ItfValue.decode] error's field path with [root]: an empty/`<root>` path (the whole
 * decoded value) becomes [root] itself, any other path is joined with a dot. Used from
 * [TypedState.check] (`"state"`) and `NondetPicks.decode`/`decodeOrNull` (`"picks.<name>"`), which
 * are public inline reified functions and so may only call public or `@PublishedApi internal` API.
 */
@PublishedApi
internal fun prefixDecodeError(root: String, e: SerializationException): SerializationException {
    val message = e.message.orEmpty()
    val prefixed = if (message.startsWith("<root>")) root + message.removePrefix("<root>") else "$root.$message"
    return SerializationException(prefixed, e)
}

private fun fullValueDiff(spec: String, impl: String): String {
    val sb = StringBuilder()
    sb.appendLine("--- specification")
    sb.appendLine("+++ implementation")
    spec.lines().forEach { sb.appendLine("-$it") }
    impl.lines().forEach { sb.appendLine("+$it") }
    return sb.toString()
}
