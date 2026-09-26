package io.github.mcbianconi.quintkonnect.example.sumtypes

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.DriverConfig
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.annotations.QuintAction
import io.github.mcbianconi.quintkonnect.annotations.QuintRun

private val prices = mapOf("soda" to 100L, "chips" to 150L)

// `lastAction` is a sum type (like docs/decisions/quint-test-needs-nondet-path.md), but `machine`
// (via statePath) keeps it out of the state comparison itself.
@QuintRun(
    spec = "src/test/resources/sumtypes/vendingmachine.qnt",
    maxSamples = 20,
)
class VendingMachineDriver : Driver {
    var credit: Long = 0
    var selected: String = ""

    override fun config(): DriverConfig = DriverConfig(statePath = listOf("machine"), nondetPath = listOf("lastAction"))

    override fun quintState(): State<VendingMachineDriver> = VendingMachineState()

    @QuintAction("Init")
    fun init() {
        credit = 0
        selected = ""
    }

    @QuintAction("Insert")
    fun insert(amount: Long) {
        credit += amount
    }

    @QuintAction("Select")
    fun select(item: String) {
        selected = item
    }

    @QuintAction("Dispense")
    fun dispense() {
        credit -= prices.getValue(selected)
        selected = ""
    }

    @QuintAction("Refund")
    fun refund() {
        credit = 0
    }
}
