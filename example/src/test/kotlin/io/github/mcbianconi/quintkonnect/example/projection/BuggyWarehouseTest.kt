package io.github.mcbianconi.quintkonnect.example.projection

import io.github.mcbianconi.quintkonnect.ReplayRunner
import io.github.mcbianconi.quintkonnect.trace.RunConfig
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

private class BuggyWarehouse(skus: Set<Long>) : Warehouse(skus) {
    override fun ship(sku: Long) {
        val qty = reserved.getValue(sku)
        stock[sku] = stock.getValue(sku) - qty
        recordShipped(qty)
        // bug: forgets to clear `reserved[sku]`, so the spec's projection of it goes stale
    }
}

class BuggyWarehouseTest {
    @Test
    fun `catches the unreserved-after-ship bug`() {
        val error = assertThrows<AssertionError> {
            ReplayRunner(
                generatorConfig = RunConfig(
                    spec = "src/test/resources/projection/warehouse.qnt",
                    seed = "42",
                    maxSamples = 10,
                    maxSteps = 20,
                ),
            ).runTest(
                driverFactory = { WarehouseDriver(newWarehouse = { BuggyWarehouse(warehouseSkus) }) },
                testName = "BuggyWarehouseDriver",
            )
        }
        assertTrue(error.message.orEmpty().contains("reserved"))
    }
}
