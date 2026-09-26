module bankledger {
  pure val accounts = Set(1, 2, 3)
  pure val amounts = Set(1, 5, 10, 25)

  var balances: int -> int

  action init = all {
    balances' = accounts.mapBy(_ => 0),
  }

  action deposit = {
    nondet account = accounts.oneOf()
    nondet amount = amounts.oneOf()
    all {
      balances' = balances.setBy(account, b => b + amount),
    }
  }

  action withdraw = {
    nondet account = accounts.oneOf()
    nondet amount = amounts.oneOf()
    all {
      balances.get(account) >= amount,
      balances' = balances.setBy(account, b => b - amount),
    }
  }

  action step = any {
    deposit,
    withdraw,
  }
}
