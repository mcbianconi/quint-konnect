package io.github.mcbianconi.quintkonnect.example.projection

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.annotations.QuintAction
import io.github.mcbianconi.quintkonnect.annotations.QuintRun

internal val warehouseSkus = setOf(1L, 2L, 3L, 4L, 5L)

@QuintRun(
    spec = "src/test/resources/projection/warehouse.qnt",
    maxSamples = 20,
    maxSteps = 20,
)
class WarehouseDriver(private val newWarehouse: () -> Warehouse = { Warehouse(warehouseSkus) }) : Driver {
    var warehouse = newWarehouse()

    override fun quintState(): State<WarehouseDriver> = WarehouseState()

    @QuintAction("init")
    fun init() {
        warehouse = newWarehouse()
    }

    @QuintAction("restock")
    fun restock(sku: Long, qty: Long) = warehouse.restock(sku, qty)

    @QuintAction("reserve")
    fun reserve(sku: Long, qty: Long) = warehouse.reserve(sku, qty)

    @QuintAction("ship")
    fun ship(sku: Long) = warehouse.ship(sku)
}
