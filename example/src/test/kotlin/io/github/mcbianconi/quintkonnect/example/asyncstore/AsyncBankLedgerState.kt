package io.github.mcbianconi.quintkonnect.example.asyncstore

import io.github.mcbianconi.quintkonnect.TypedState
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

@Serializable
data class BankLedgerValue(val balances: Map<Long, Long>)

class AsyncBankLedgerState : TypedState<AsyncBankLedgerDriver, BankLedgerValue>(serializer()) {
    override fun extractFromDriver(driver: AsyncBankLedgerDriver): BankLedgerValue =
        runBlocking { BankLedgerValue(balances = driver.ledger.snapshot()) }
}
