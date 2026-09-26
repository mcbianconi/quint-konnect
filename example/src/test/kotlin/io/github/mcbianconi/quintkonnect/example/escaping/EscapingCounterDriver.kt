package io.github.mcbianconi.quintkonnect.example.escaping

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.annotations.QuintAction
import io.github.mcbianconi.quintkonnect.annotations.QuintRun

// Fixture for qk-gu38: the spec path and one action name below contain
// characters ($ and ") that must be escaped by the KSP generators for the
// generated Kotlin file to compile.
@QuintRun(
    spec = "src/test/resources/escaping/counter\$1.qnt",
    maxSamples = 3,
    maxSteps = 5,
)
class EscapingCounterDriver : Driver {
    var count = 0L

    override fun quintState(): State<EscapingCounterDriver> = EscapingCounterState()

    @QuintAction("init")
    fun init() {
        count = 0
    }

    @QuintAction("increment")
    fun increment() {
        count += 1
    }

    // Never taken by the spec above; only here to exercise the StepMethodGenerator
    // escaping of a "$"/"\"" action name.
    @QuintAction("never\$\"taken")
    fun neverTaken() {}
}
