package io.github.mcbianconi.quintkonnect.example.asyncstore

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.serializer

class AsyncBankLedgerState : TypedState<AsyncBankLedgerDriver, BankledgerSpec.State>(serializer()) {
    override fun extractFromDriver(driver: AsyncBankLedgerDriver): BankledgerSpec.State =
        runBlocking { BankledgerSpec.State(balances = driver.ledger.snapshot()) }
}
