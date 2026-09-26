package io.github.mcbianconi.quintkonnect.example.quinttest

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.DriverConfig
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.annotations.QuintAction
import io.github.mcbianconi.quintkonnect.annotations.QuintTest

// `quint test` writes no mbt:: variables; see docs/decisions/quint-test-needs-nondet-path.md
@QuintTest(
    spec = "src/test/resources/quinttest/counter.qnt",
    test = "happyTest",
)
class CounterDriver : Driver {
    var count = 0L

    override fun config(): DriverConfig = DriverConfig(nondetPath = listOf("lastAction"))

    override fun quintState(): State<*> = CounterState()

    @QuintAction("Init")
    fun init() {
        count = 0
    }

    @QuintAction("Add")
    fun add(n: Long) {
        count += n
    }
}
