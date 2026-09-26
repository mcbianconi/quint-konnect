package io.github.mcbianconi.quintkonnect.example.asyncstore

import io.github.mcbianconi.quintkonnect.Driver
import io.github.mcbianconi.quintkonnect.State
import io.github.mcbianconi.quintkonnect.annotations.QuintAction
import io.github.mcbianconi.quintkonnect.annotations.QuintRun

@QuintRun(
    spec = "src/test/resources/asyncstore/bankledger.qnt",
    maxSamples = 20,
)
class AsyncBankLedgerDriver : Driver {
    val ledger = AsyncBankLedger(setOf(1L, 2L, 3L))

    override fun quintState(): State<AsyncBankLedgerDriver> = AsyncBankLedgerState()

    @QuintAction("init")
    fun init() {}

    @QuintAction("deposit")
    suspend fun deposit(account: Long, amount: Long) = ledger.deposit(account, amount)

    @QuintAction("withdraw")
    suspend fun withdraw(account: Long, amount: Long) = ledger.withdraw(account, amount)
}
