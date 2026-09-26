module vendingmachine {
  type Action = Init | Insert({ amount: int }) | Select({ item: str }) | Dispense | Refund

  var machine: { credit: int, selected: str }
  var lastAction: Action

  pure val items = Set("soda", "chips")
  pure val prices = Map("soda" -> 100, "chips" -> 150)

  action init = all {
    machine' = { credit: 0, selected: "" },
    lastAction' = Init,
  }

  action insert = {
    nondet amount = Set(25, 50, 100).oneOf()
    all {
      machine' = machine.with("credit", machine.credit + amount),
      lastAction' = Insert({ amount: amount }),
    }
  }

  action selectItem = {
    nondet chosen = items.oneOf()
    all {
      machine' = machine.with("selected", chosen),
      lastAction' = Select({ item: chosen }),
    }
  }

  action dispense = all {
    machine.selected != "",
    machine.credit >= prices.get(machine.selected),
    machine' = { credit: machine.credit - prices.get(machine.selected), selected: "" },
    lastAction' = Dispense,
  }

  action refund = all {
    machine.credit > 0,
    machine' = machine.with("credit", 0),
    lastAction' = Refund,
  }

  action step = any {
    insert,
    selectItem,
    dispense,
    refund,
  }
}
