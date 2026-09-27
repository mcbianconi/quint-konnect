package io.github.mcbianconi.quintkonnect.example.invariants

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.serializer

class InvariantCounterState : TypedState<InvariantCounterDriver, CounterSpec.State>(serializer()) {
    override fun extractFromDriver(driver: InvariantCounterDriver): CounterSpec.State =
        CounterSpec.State(n = driver.n)
}
