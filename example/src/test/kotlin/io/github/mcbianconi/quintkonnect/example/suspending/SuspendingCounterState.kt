package io.github.mcbianconi.quintkonnect.example.suspending

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class SuspendingCounterValue(val count: Long)

class SuspendingCounterState : TypedState<SuspendingCounterDriver, SuspendingCounterValue>(serializer()) {
    override fun extractFromDriver(driver: SuspendingCounterDriver): SuspendingCounterValue =
        SuspendingCounterValue(count = driver.count)
}
