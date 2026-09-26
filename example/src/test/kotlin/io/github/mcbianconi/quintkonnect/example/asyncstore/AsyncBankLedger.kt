package io.github.mcbianconi.quintkonnect.example.asyncstore

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AsyncBankLedger(accountIds: Set<Long>) {
    private val mutex = Mutex()
    private val balances: MutableMap<Long, Long> = accountIds.associateWithTo(mutableMapOf()) { 0L }

    suspend fun deposit(account: Long, amount: Long) = mutex.withLock {
        delay(1)
        balances[account] = balances.getValue(account) + amount
    }

    suspend fun withdraw(account: Long, amount: Long) = mutex.withLock {
        delay(1)
        val balance = balances.getValue(account)
        check(balance >= amount) { "insufficient funds in account $account: has $balance, wants $amount" }
        balances[account] = balance - amount
    }

    suspend fun snapshot(): Map<Long, Long> = mutex.withLock { balances.toMap() }
}
