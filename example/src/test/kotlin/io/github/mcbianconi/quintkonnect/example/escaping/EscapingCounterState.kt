package io.github.mcbianconi.quintkonnect.example.escaping

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class CounterState(val count: Long)

class EscapingCounterState : TypedState<EscapingCounterDriver, CounterState>(serializer()) {
    override fun extractFromDriver(driver: EscapingCounterDriver): CounterState =
        CounterState(count = driver.count)
}
