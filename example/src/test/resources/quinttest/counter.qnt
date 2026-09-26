module counter {
  type Action = Init | Add({ n: int })

  var count: int
  var lastAction: Action

  action init = all {
    count' = 0,
    lastAction' = Init,
  }

  action add(n) = all {
    count' = count + n,
    lastAction' = Add({ n: n }),
  }

  action step = any {
    nondet n = Set(1, 2, 3).oneOf()
    add(n),
  }

  run happyTest = init.then(step).then(step)
}
