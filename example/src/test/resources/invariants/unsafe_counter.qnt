module unsafe_counter {
  var n: int

  action init = { n' = 0 }
  action step = { n' = n + 1 }

  val safe = n < 3
}
