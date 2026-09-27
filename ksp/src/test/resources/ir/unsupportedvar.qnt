// Fixture for SpecTypesGeneratorTest's addDriverState coverage (qk-ymex): `pair` has no
// decodable Kotlin shape (a heterogeneous tuple), so `State` itself is skipped, but a driver that
// `ignore`s `pair` still gets its own `<Driver>State` over `value` alone.
// Regenerate its IR with:
//   quint typecheck --out ksp/src/test/resources/ir/unsupportedvar.ir.json ksp/src/test/resources/ir/unsupportedvar.qnt
module unsupportedvar {
  type Pair = (int, str)

  var value: int
  var pair: Pair

  action init = all {
    value' = 0,
    pair' = (0, "x"),
  }

  action step = any {
    all {
      value' = value + 1,
      pair' = pair,
    },
  }
}
