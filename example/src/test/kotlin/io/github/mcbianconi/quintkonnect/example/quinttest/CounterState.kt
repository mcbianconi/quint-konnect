package io.github.mcbianconi.quintkonnect.example.quinttest

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

// Hand-written rather than CounterSpec.State, which also has `lastAction` (the nondetPath carrier
// this driver doesn't model); see qk-ymex.
@Serializable
data class CounterValue(val count: Long)

class CounterState : TypedState<CounterDriver, CounterValue>(serializer()) {
    override fun extractFromDriver(driver: CounterDriver): CounterValue = CounterValue(count = driver.count)
}
