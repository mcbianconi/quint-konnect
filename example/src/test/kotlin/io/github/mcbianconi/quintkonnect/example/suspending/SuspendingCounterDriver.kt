package io.github.mcbianconi.quintkonnect.example.suspending

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.DriverConfig
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.annotations.QuintAction
import io.github.mcbianconi.quintkonnect.annotations.QuintTest
import kotlinx.coroutines.delay

// qk-33ky: exercises a suspend @QuintAction end to end (generatedStep wraps its call in
// kotlinx.coroutines.runBlocking); reuses quinttest/counter.qnt (see CounterDriver).
@QuintTest(
    spec = "src/test/resources/quinttest/counter.qnt",
    test = "happyTest",
)
class SuspendingCounterDriver : Driver {
    var count = 0L

    override fun config(): DriverConfig = DriverConfig(nondetPath = listOf("lastAction"))

    override fun quintState(): State<SuspendingCounterDriver> = SuspendingCounterState()

    @QuintAction("Init")
    suspend fun init() {
        delay(1)
        count = 0
    }

    @QuintAction("Add")
    suspend fun add(n: Long) {
        delay(1)
        count += n
    }
}
