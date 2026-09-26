package io.github.mcbianconi.quintkonnect.example.invariants

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.annotations.QuintAction
import io.github.mcbianconi.quintkonnect.annotations.QuintRun

@QuintRun(
    spec = "src/test/resources/invariants/counter.qnt",
    invariants = ["safe"],
    maxSamples = 10,
    maxSteps = 10,
)
class InvariantCounterDriver : Driver {
    var n: Long = 0

    override fun quintState(): State<InvariantCounterDriver> = InvariantCounterState()

    @QuintAction("init")
    fun init() {
        n = 0
    }

    @QuintAction("increment")
    fun increment() {
        n += 1
    }

    @QuintAction("reset")
    fun reset() {
        n = 0
    }
}
