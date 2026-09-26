package io.github.mcbianconi.quintkonnect.example.projection

open class Warehouse(skus: Set<Long>) {
    val stock: MutableMap<Long, Long> = skus.associateWithTo(mutableMapOf()) { 50L }
    val reserved: MutableMap<Long, Long> = skus.associateWithTo(mutableMapOf()) { 0L }

    // A batched metrics counter, only flushed in multiples of 5: it can lag the true shipped
    // total by up to 4 units at any point, which WarehouseState.compareField tolerates.
    private var telemetryBuffer: Long = 0
    var shippedTelemetry: Long = 0
        private set

    fun restock(sku: Long, qty: Long) {
        stock[sku] = stock.getValue(sku) + qty
    }

    fun reserve(sku: Long, qty: Long) {
        reserved[sku] = reserved.getValue(sku) + qty
    }

    open fun ship(sku: Long) {
        val qty = reserved.getValue(sku)
        stock[sku] = stock.getValue(sku) - qty
        recordShipped(qty)
        reserved[sku] = 0
    }

    protected fun recordShipped(qty: Long) {
        telemetryBuffer += qty
        val flushed = (telemetryBuffer / 5) * 5
        shippedTelemetry += flushed
        telemetryBuffer -= flushed
    }
}
