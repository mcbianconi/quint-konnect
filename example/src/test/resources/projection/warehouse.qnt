module warehouse {
  pure val skus = Set(1, 2, 3, 4, 5)
  pure val bins = Map(1 -> 100, 2 -> 100, 3 -> 200, 4 -> 200, 5 -> 300)

  var stock: int -> int
  var reserved: int -> int
  var binOf: int -> int
  var reservationLog: Set[int]
  var shipped: int

  action init = all {
    stock' = skus.mapBy(_ => 50),
    reserved' = skus.mapBy(_ => 0),
    binOf' = bins,
    reservationLog' = Set(),
    shipped' = 0,
  }

  action restock = {
    nondet sku = skus.oneOf()
    nondet qty = Set(5, 10, 20).oneOf()
    all {
      stock' = stock.setBy(sku, s => s + qty),
      reserved' = reserved,
      binOf' = binOf,
      reservationLog' = reservationLog,
      shipped' = shipped,
    }
  }

  action reserve = {
    nondet sku = skus.oneOf()
    nondet qty = Set(1, 2, 3).oneOf()
    all {
      stock.get(sku) - reserved.get(sku) >= qty,
      reserved' = reserved.setBy(sku, r => r + qty),
      stock' = stock,
      binOf' = binOf,
      reservationLog' = reservationLog.union(Set(sku)),
      shipped' = shipped,
    }
  }

  action ship = {
    nondet sku = skus.oneOf()
    all {
      reserved.get(sku) > 0,
      stock' = stock.setBy(sku, s => s - reserved.get(sku)),
      shipped' = shipped + reserved.get(sku),
      reserved' = reserved.setBy(sku, _ => 0),
      binOf' = binOf,
      reservationLog' = reservationLog,
    }
  }

  action step = any {
    restock,
    reserve,
    ship,
  }
}
