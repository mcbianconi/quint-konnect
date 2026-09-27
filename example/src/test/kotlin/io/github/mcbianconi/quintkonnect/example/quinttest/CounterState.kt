package io.github.mcbianconi.quintkonnect.example.quinttest

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.serializer

// CounterSpec.CounterDriverState leaves out lastAction (the nondetPath carrier this driver
// doesn't model) via CounterDriver's `ignore` (qk-ymex), instead of a hand-written class.
class CounterState : TypedState<CounterDriver, CounterSpec.CounterDriverState>(serializer()) {
    override fun extractFromDriver(driver: CounterDriver): CounterSpec.CounterDriverState =
        CounterSpec.CounterDriverState(count = driver.count)
}
