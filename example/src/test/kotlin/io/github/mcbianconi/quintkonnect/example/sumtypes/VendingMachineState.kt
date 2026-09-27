package io.github.mcbianconi.quintkonnect.example.sumtypes

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.serializer

// `statePath = machine` (VendingMachineDriver.config) compares only that record, so this checks
// VendingmachineSpec.Machine rather than the whole VendingmachineSpec.State.
class VendingMachineState : TypedState<VendingMachineDriver, VendingmachineSpec.Machine>(serializer()) {
    override fun extractFromDriver(driver: VendingMachineDriver): VendingmachineSpec.Machine =
        VendingmachineSpec.Machine(credit = driver.credit, selected = driver.selected)
}
