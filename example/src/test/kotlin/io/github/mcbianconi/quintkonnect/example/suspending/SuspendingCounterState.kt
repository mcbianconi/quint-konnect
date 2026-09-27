package io.github.mcbianconi.quintkonnect.example.suspending

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.serialization.serializer

// CounterSpec.SuspendingCounterDriverState leaves out lastAction (the nondetPath carrier this
// driver doesn't model) via SuspendingCounterDriver's `ignore` (qk-ymex), instead of a
// hand-written class.
class SuspendingCounterState : TypedState<SuspendingCounterDriver, CounterSpec.SuspendingCounterDriverState>(serializer()) {
    override fun extractFromDriver(driver: SuspendingCounterDriver): CounterSpec.SuspendingCounterDriverState =
        CounterSpec.SuspendingCounterDriverState(count = driver.count)
}
