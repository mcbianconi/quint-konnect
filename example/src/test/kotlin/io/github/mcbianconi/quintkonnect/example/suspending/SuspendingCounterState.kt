package io.github.mcbianconi.quintkonnect.example.suspending

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

// Hand-written rather than CounterSpec.State, which also has `lastAction` (the nondetPath carrier
// this driver doesn't model); see qk-ymex.
@Serializable
data class SuspendingCounterValue(val count: Long)

class SuspendingCounterState : TypedState<SuspendingCounterDriver, SuspendingCounterValue>(serializer()) {
    override fun extractFromDriver(driver: SuspendingCounterDriver): SuspendingCounterValue =
        SuspendingCounterValue(count = driver.count)
}
