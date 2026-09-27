package io.github.mcbianconi.quintkonnect.example.projection

import io.github.mcbianconi.quintkonnect.TypedState
import io.github.mcbianconi.quintkonnect.annotations.QuintIgnore
import kotlin.math.abs
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

// The spec tracks bin placement (`binOf`) and a growing reservation history (`reservationLog`)
// that this driver never models. `@QuintIgnore`d fields still need a value to build this class,
// even though TypedState.check never compares them. Hand-written rather than WarehouseSpec.State
// (or a `WarehouseDriver`-specific projection, `@QuintRun(ignore = [...])`, qk-ymex): both of
// those drop an unmodeled field entirely, whereas this demonstrates `@QuintIgnore` keeping the
// field (with a placeholder value) and still never failing the check on it.
@Serializable
data class WarehouseValue(
    val stock: Map<Long, Long>,
    val reserved: Map<Long, Long>,
    @QuintIgnore val binOf: Map<Long, Long> = emptyMap(),
    @QuintIgnore val reservationLog: Set<Long> = emptySet(),
    val shipped: Long,
)

class WarehouseState : TypedState<WarehouseDriver, WarehouseValue>(serializer()) {
    override fun extractFromDriver(driver: WarehouseDriver): WarehouseValue =
        WarehouseValue(
            stock = driver.warehouse.stock.toMap(),
            reserved = driver.warehouse.reserved.toMap(),
            shipped = driver.warehouse.shippedTelemetry,
        )

    // `shippedTelemetry` is a batched counter (Warehouse.recordShipped) that can lag the spec's
    // exact `shipped` count by up to 4 units.
    override fun compareField(path: String, spec: String, impl: String): Boolean? {
        if (path != "shipped") return null
        val specShipped = spec.toLongOrNull() ?: return null
        val implShipped = impl.toLongOrNull() ?: return null
        return abs(specShipped - implShipped) <= 4
    }
}
