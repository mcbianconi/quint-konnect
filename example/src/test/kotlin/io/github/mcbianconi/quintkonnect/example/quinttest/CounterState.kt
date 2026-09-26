package io.github.mcbianconi.quintkonnect.example.quinttest

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class CounterValue(val count: Long)

class CounterState : TypedState<CounterDriver, CounterValue>(serializer()) {
    override fun extractFromDriver(driver: CounterDriver): CounterValue = CounterValue(count = driver.count)
}
