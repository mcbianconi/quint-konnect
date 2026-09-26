package io.github.mcbianconi.quintkonnect.example.sumtypes

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class VendingMachineValue(val credit: Long, val selected: String)

class VendingMachineState : TypedState<VendingMachineDriver, VendingMachineValue>(serializer()) {
    override fun extractFromDriver(driver: VendingMachineDriver): VendingMachineValue =
        VendingMachineValue(credit = driver.credit, selected = driver.selected)
}
