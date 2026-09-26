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

    /**
     * Overrides the default comparison for the field at [path] (dot/bracket-joined the way this
     * class's diff messages name it, e.g. `"count"`, `"cells.(1, 2)"`, `"items[0]"`, or `"<root>"`
     * for the whole state), given both sides' rendered value at that path. Returning `null` (the
     * default) falls through to the default structural comparison; returning `true`/`false` treats
     * the field, and anything nested under it, as equal/unequal without comparing it further.
     */
    public open fun compareField(path: String, spec: String, impl: String): Boolean? = null

    override fun check(driver: D, specValue: ItfValue) {
        val specState = try {
            specValue.decode(serializer)
        } catch (e: SerializationException) {
            throw prefixDecodeError("state", e)
        }
        val driverState = extractFromDriver(driver)

        // buildFieldDiff drives the mismatch verdict: it's the only comparison that can honour
        // @QuintIgnore and compareField. Falls back to equals() only if it throws (which it
        // shouldn't for any serializer this module supports) - that fallback can't honour either.
        val fieldDiff = try {
            buildFieldDiff(serializer, specState, driverState, ::compareField)
        } catch (e: Exception) {
            null
        }
        val mismatched = if (fieldDiff != null) fieldDiff.isNotEmpty() else specState != driverState

        if (mismatched) {
            val fullValues = fullValueDiff(specState.toString(), driverState.toString())
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
