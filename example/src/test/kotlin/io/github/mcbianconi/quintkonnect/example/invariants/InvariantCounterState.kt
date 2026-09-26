package io.github.mcbianconi.quintkonnect.example.invariants

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class InvariantCounterValue(val n: Long)

class InvariantCounterState : TypedState<InvariantCounterDriver, InvariantCounterValue>(serializer()) {
    override fun extractFromDriver(driver: InvariantCounterDriver): InvariantCounterValue =
        InvariantCounterValue(n = driver.n)
}
