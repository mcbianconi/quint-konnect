module counter {
  var n: int

  action init = { n' = 0 }
  action increment = { n' = n + 1 }
  action reset = { n' = 0 }

  action step = any {
    increment,
    reset,
  }

  val safe = n >= 0 and n < 100
}
